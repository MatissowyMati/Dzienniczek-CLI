package io.github.matissowymati.dzienniczek.api.prometheus

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.github.matissowymati.dzienniczek.api.network.ProviderTls
import io.github.matissowymati.dzienniczek.api.prometheus.models.PrometheusMessage
import io.github.matissowymati.dzienniczek.api.prometheus.models.PrometheusMessageDetails
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.parametersOf
import io.ktor.serialization.kotlinx.json.json as ktorJson
import kotlinx.serialization.json.Json
import net.thauvin.erik.urlencoder.UrlEncoderUtil

class PrometheusMessagesApi(
    val tenant: String,
    val login: String? = null,
    val password: String? = null,
    private var initialCookies: List<Cookie>? = null
) {
    private val ssoBaseUrl = "https://dziennik-logowanie.vulcan.net.pl"
    private val messagesBaseUrl = "https://wiadomosci.eduvulcan.pl"
    
    private var antiForgeryToken: String = ""
    private var appGuid: String = ""
    private var isInitialized: Boolean = false
    
    private val json = Json { ignoreUnknownKeys = true }
    private val cookieStorage = AcceptAllCookiesStorage()
    
    private val httpClient = HttpClient(CIO) {
        followRedirects = true
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
            agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
            }
        }
    }
    
    suspend fun initialize() {
        if (isInitialized) return

        var currentCookies = initialCookies
        if (currentCookies == null) {
            if (login != null && password != null) {
                val helper = PrometheusLoginHelper()
                val result = helper.login(login, password, "Dzienniczek CLI")
                currentCookies = result.cookies
            } else {
                throw IllegalStateException("Brak ciasteczek i brak danych logowania")
            }
        }
        
        // Załaduj początkowe ciasteczka.
        currentCookies.forEach { cookie ->
            val domain = cookie.domain?.removePrefix(".") ?: "eduvulcan.pl"
            val url = Url("https://$domain")
            cookieStorage.addCookie(url, cookie)
        }
        
        val prometheusEncoded = UrlEncoderUtil.encode("https://eduvulcan.pl")
        val ssoEncoded = UrlEncoderUtil.encode(ssoBaseUrl)
        val studentEncoded = UrlEncoderUtil.encode("https://uczen.eduvulcan.pl")

        // Pierwszy przepływ SSO uwierzytelnia klienta w domenie SSO.
        val firstAuthUrl = "https://eduvulcan.pl/fs/ls?wa=wsignin1.0&wtrealm=$ssoEncoded%2F${tenant}%2FFs%2FLs%3Fwa%3Dwsignin1.0%26wtrealm%3D$studentEncoded%2F${tenant}%2FAccount%2FLogin%3FreturnUrl%3D$prometheusEncoded%26wctx%3Dauth%3DstudentEV%26nslo%3D1&wctx=nslo%3D1"
        authorizePrometheus(firstAuthUrl)

        // Drugi przepływ SSO uwierzytelnia klienta w domenie wiadomości.
        val authorizeUrl = "$ssoBaseUrl/$tenant/Fs/Ls?wa=wsignin1.0&wtrealm=$messagesBaseUrl/$tenant/Account/Login?returnUrl=/$tenant/App&wctx=auth=studentEV&nslo=1"
        authorizePrometheus(authorizeUrl)
        
        // Pobierz tokeny z aplikacji.
        val appScript = Ksoup.parse(httpClient.get("$messagesBaseUrl/$tenant/App").bodyAsText())
            .select("script").firstOrNull()?.html() ?: ""
            
        antiForgeryToken = Regex("antiForgeryToken: '(.*?)'").find(appScript)?.groupValues?.get(1) ?: ""
        appGuid = Regex("appGuid: '(.*?)'").find(appScript)?.groupValues?.get(1) ?: ""
        
        isInitialized = true
    }
    
    private suspend fun authorizePrometheus(url: String) {
        val response1 = httpClient.get(url)
        val document = Ksoup.parse(response1.bodyAsText())
        val res1 = findAndSubmitForm(document)
            ?: throw IllegalStateException("Błąd SSO pod adresem $url — nie znaleziono formularza. Tytuł strony: ${document.title()}")
        val doc2 = Ksoup.parse(res1.bodyAsText())
        findAndSubmitForm(doc2)
            ?: throw IllegalStateException("Błąd SSO w formularzu dodatkowym — nie znaleziono formularza. Tytuł strony: ${doc2.title()}")
    }
    
    private suspend fun findAndSubmitForm(document: Document): HttpResponse? {
        val form = document.forms().firstOrNull() ?: return null
        val fields = form.children().select("input[type=\"hidden\"]")
            .associate { it.attr("name") to listOf(it.value()) }
            
        return httpClient.submitForm(
            url = form.attr("action"),
            formParameters = parametersOf(fields)
        )
    }
    
    suspend fun getReceivedMessages(mailboxKey: String, pageSize: Int = 50, lastMessageId: Int = 0): List<PrometheusMessage> {
        return fetchMessages("/api/OdebraneSkrzynka", mailboxKey, pageSize, lastMessageId)
    }
    
    suspend fun getSentMessages(mailboxKey: String, pageSize: Int = 50, lastMessageId: Int = 0): List<PrometheusMessage> {
        return fetchMessages("/api/WyslaneSkrzynka", mailboxKey, pageSize, lastMessageId)
    }
    
    suspend fun getDeletedMessages(mailboxKey: String, pageSize: Int = 50, lastMessageId: Int = 0): List<PrometheusMessage> {
        return fetchMessages("/api/UsunieteSkrzynka", mailboxKey, pageSize, lastMessageId)
    }
    
    private suspend fun fetchMessages(endpoint: String, mailboxKey: String, pageSize: Int, lastMessageId: Int): List<PrometheusMessage> {
        val response = httpClient.get("$messagesBaseUrl/$tenant$endpoint") {
            parameter("globalKeySkrzynka", mailboxKey)
            parameter("idLastWiadomosc", lastMessageId)
            parameter("pageSize", pageSize)
            header("X-V-AppGuid", appGuid)
            header("X-V-RequestVerificationToken", antiForgeryToken)
            contentType(ContentType.Application.Json)
        }
        return response.body()
    }
    
    suspend fun getMessageDetails(apiGlobalKey: String): PrometheusMessageDetails {
        val response = httpClient.get("$messagesBaseUrl/$tenant/api/WiadomoscSzczegoly") {
            parameter("apiGlobalKey", apiGlobalKey)
            contentType(ContentType.Application.Json)
        }
        return response.body()
    }
}
