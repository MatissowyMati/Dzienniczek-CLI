package io.github.matissowymati.dzienniczek.api.hebe

import io.github.matissowymati.dzienniczek.api.hebe.credentials.ICredential
import io.ktor.client.HttpClient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val HEBE_APP_NAME = "DzienniczekPlus 2.0"
private const val HEBE_APP_VERSION = "26.04.00 (G)"
private const val HEBE_APP_VERSION_CODE = "941"

private val TOKEN_PREFIXES = mapOf(
    "3S1" to "https://lekcjaplus.vulcan.net.pl",
    "TA1" to "https://uonetplus-komunikacja.umt.tarnow.pl",
    "OP1" to "https://uonetplus-komunikacja.eszkola.opolskie.pl",
    "RZ1" to "https://uonetplus-komunikacja.resman.pl",
    "GD1" to "https://uonetplus-komunikacja.edu.gdansk.pl",
    "KA1" to "https://uonetplus-komunikacja.mcuw.katowice.eu",
    "KA2" to "https://uonetplus-komunikacja-test.mcuw.katowice.eu",
    "LU1" to "https://uonetplus-komunikacja.edu.lublin.eu",
    "LU2" to "https://test-uonetplus-komunikacja.edu.lublin.eu",
    "P03" to "https://efeb-komunikacja-pro-efebmobile.pro.vulcan.pl",
    "P01" to "http://efeb-komunikacja.pro-hudson.win.vulcan.pl",
    "P02" to "http://efeb-komunikacja.pro-hudsonrc.win.vulcan.pl",
    "P90" to "http://efeb-komunikacja-pro-mwujakowska.neo.win.vulcan.pl",
    "KO1" to "https://uonetplus-komunikacja.eduportal.koszalin.pl"
)

/**
 * Klient API VULCAN Hebe. Rejestruje urządzenie za pomocą tokenu zabezpieczającego i kodu PIN.
 *
 * Użycie:
 * 1. Utwórz dane urządzenia: `RsaCredential.createNew("Android", "Moje urządzenie")`
 * 2. Utwórz klienta API: `VulcanApi(credential, httpClient)`
 * 3. Zarejestruj urządzenie: `api.registerByToken(token, pin, tenant)`
 * 4. Wywołaj dowolny punkt końcowy z [DzienniczekApi].
 */
class VulcanApi(
    credential: ICredential,
    httpClient: HttpClient
) : DzienniczekApi(
    credential,
    VulcanHttpClient(credential, HEBE_APP_NAME, HEBE_APP_VERSION, HEBE_APP_VERSION_CODE, httpClient)
) {

    /**
     * Rejestruje urządzenie za pomocą tokenu zabezpieczającego i kodu PIN.
     *
     * @param securityToken trzyznakowy prefiks tokenu i pozostałe znaki (np. "3S1ABCDE...")
     * @param pin rejestracyjny kod PIN wyświetlany w portalu VULCAN
     * @param tenant symbol instancji szkoły
     * @return adres REST przypisany po udanej rejestracji
     */
    suspend fun registerByToken(securityToken: String, pin: String, tenant: String): String {
        val token = securityToken.uppercase()
        val baseUrl = TOKEN_PREFIXES[token.take(3)]
            ?: throw WrongTokenException("Nieznany prefiks tokenu: ${token.take(3)}")
        val restUrl = "$baseUrl/$tenant/api"

        vulcanHttpClient.request(
            method = "POST",
            endpoint = "mobile/register/token",
            restUrl = restUrl,
            payload = buildJsonObject {
                put("OS", credential.deviceOs)
                put("Certificate", credential.certificate)
                put("CertificateType", credential.type)
                put("DeviceModel", credential.deviceModel)
                put("SelfIdentifier", credential.deviceId)
                put("CertificateThumbprint", credential.fingerprint)
                put("SecurityToken", token)
                put("PIN", pin)
            }
        )

        credential.restUrl = restUrl
        return restUrl
    }
}
