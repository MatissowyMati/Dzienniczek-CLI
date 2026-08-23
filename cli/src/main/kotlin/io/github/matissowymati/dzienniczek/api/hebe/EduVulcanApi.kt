package io.github.matissowymati.dzienniczek.api.hebe

import io.github.matissowymati.dzienniczek.api.hebe.credentials.ICredential
import io.ktor.client.HttpClient
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val HEBECE_APP_NAME = "DzienniczekPlus 3.0"
private const val HEBECE_APP_VERSION = "26.04.01 (G)"
private const val HEBECE_APP_VERSION_CODE = "946"
private const val HEBECE_API_BASE_URL = "https://lekcjaplus.vulcan.net.pl"

/**
 * Klient API VULCAN Hebe CE. Rejestruje urządzenie za pomocą listy tokenów JWT.
 *
 * Użycie:
 * 1. Utwórz dane urządzenia: `RsaCredential.createNew("Android", "Moje urządzenie")`
 * 2. Utwórz klienta API: `EduVulcanApi(credential, httpClient)`
 * 3. Zarejestruj urządzenie: `api.registerByJwt(tokens, tenant)`
 * 4. Wywołaj dowolny punkt końcowy z [DzienniczekApi].
 */
class EduVulcanApi(
    credential: ICredential,
    httpClient: HttpClient
) : DzienniczekApi(
    credential,
    VulcanHttpClient(credential, HEBECE_APP_NAME, HEBECE_APP_VERSION, HEBECE_APP_VERSION_CODE, httpClient)
) {

    /**
     * Rejestruje urządzenie za pomocą listy tokenów JWT uzyskanych z portalu VULCAN.
     *
     * @param tokens lista tokenów JWT
     * @param tenant symbol instancji szkoły
     * @return adres REST przypisany po udanej rejestracji
     */
    suspend fun registerByJwt(tokens: List<String>, tenant: String): String {
        val restUrl = "$HEBECE_API_BASE_URL/$tenant/api"

        vulcanHttpClient.request(
            method = "POST",
            endpoint = "mobile/register/jwt",
            restUrl = restUrl,
            payload = buildJsonObject {
                put("OS", credential.deviceOs)
                put("Certificate", credential.certificate)
                put("CertificateType", credential.type)
                put("DeviceModel", credential.deviceModel)
                put("SelfIdentifier", credential.deviceId)
                put("CertificateThumbprint", credential.fingerprint)
                putJsonArray("Tokens") { tokens.forEach { add(it) } }
            }
        )

        credential.restUrl = restUrl
        return restUrl
    }
}
