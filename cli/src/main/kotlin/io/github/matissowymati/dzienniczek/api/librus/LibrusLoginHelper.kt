package io.github.matissowymati.dzienniczek.api.librus

import com.fleeksoft.ksoup.Ksoup
import io.github.matissowymati.dzienniczek.api.network.ProviderTls
import io.github.matissowymati.dzienniczek.api.librus.models.LibrusTokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json as ktorJson
import kotlinx.serialization.json.Json
import net.thauvin.erik.urlencoder.UrlEncoderUtil

class LibrusLoginHelper {

    private val cookieStorage = AcceptAllCookiesStorage()
    private val json = Json { ignoreUnknownKeys = true }

    private fun createClient(followRedirects: Boolean): HttpClient = HttpClient(CIO) {
        this.followRedirects = followRedirects
        engine { https { trustManager = ProviderTls.trustManager } }

        install(HttpTimeout) {
            requestTimeoutMillis = 20000
            connectTimeoutMillis = 20000
            socketTimeoutMillis = 20000
        }
        install(HttpCookies) {
            storage = cookieStorage
        }
        install(ContentNegotiation) {
            ktorJson(json)
        }
        install(UserAgent) {
            agent = LibrusConstants.USER_AGENT
        }
        defaultRequest {
            header("X-Requested-With", LibrusConstants.HEADER)
        }
    }

    private val httpClient = createClient(followRedirects = true)
    private val noRedirectClient = createClient(followRedirects = false)

    suspend fun login(email: String, password: String): LibrusTokenResponse {
        // 1. Initial authorize request to get CSRF and form parameters
        val authPageResponse = httpClient.get(LibrusConstants.AUTHORIZE_URL)
        val authPageHtml = authPageResponse.bodyAsText()
        
        if ("robotem" in authPageHtml || "g-recaptcha" in authPageHtml) {
             throw IllegalStateException("Captcha required by Librus Portal")
        }

        val doc = Ksoup.parse(authPageHtml)
        val csrfToken = doc.selectFirst("meta[name=csrf-token]")?.attr("content")
        val form = doc.selectFirst("form[action*=login]")
        val loginUrl = form?.attr("action") ?: LibrusConstants.LOGIN_URL
        
        val hiddenInputs = form?.select("input[type=hidden]")?.associate {
            it.attr("name") to it.attr("value")
        } ?: emptyMap()

        // 2. POST login data
        val loginParams = Parameters.build {
            append("email", email)
            append("password", password)
            hiddenInputs.forEach { (k, v) -> append(k, v) }
        }

        val loginResponse = noRedirectClient.submitForm(
            url = loginUrl,
            formParameters = loginParams
        ) {
            if (csrfToken != null) {
                header("X-CSRF-TOKEN", csrfToken)
            }
            header("Referer", LibrusConstants.AUTHORIZE_URL)
        }

        // 3. Follow redirects to find the code
        var currentLocation = loginResponse.headers[HttpHeaders.Location]
            ?: throw IllegalStateException("Login failed: no redirect location. Check credentials.")

        var authCode: String? = null
        
        // Follow max 5 redirects
        repeat(5) {
            if (authCode != null) return@repeat
            
            if (currentLocation.startsWith(LibrusConstants.REDIRECT_URL)) {
                authCode = currentLocation.substringAfter("code=").substringBefore("&")
                return@repeat
            }

            val resp = noRedirectClient.get(currentLocation) {
                header("Referer", LibrusConstants.AUTHORIZE_URL)
            }
            currentLocation = resp.headers[HttpHeaders.Location] ?: return@repeat
        }

        val finalCode = authCode ?: throw IllegalStateException("Could not obtain auth code from Librus")

        // 4. Exchange code for token
        val tokenResponse: LibrusTokenResponse = httpClient.submitForm(
            url = LibrusConstants.TOKEN_URL,
            formParameters = Parameters.build {
                append("client_id", LibrusConstants.CLIENT_ID)
                append("grant_type", "authorization_code")
                append("code", finalCode)
                append("redirect_uri", LibrusConstants.REDIRECT_URL)
            }
        ).bodyAsText().let { json.decodeFromString(it) }

        return tokenResponse
    }
}
