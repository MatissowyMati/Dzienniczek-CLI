package io.github.matissowymati.dzienniczek.api.network

import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object ProviderTls {
    val trustManager: X509TrustManager by lazy {
        val system = trustManager(null)
        val certificate = ProviderTls::class.java.getResourceAsStream("/certs/certum-trusted-root-ca.pem")
            ?.use { CertificateFactory.getInstance("X.509").generateCertificate(it) }
            ?: error("Bundled Certum root certificate is missing")
        val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setCertificateEntry("certum-trusted-root-ca", certificate)
        }
        val bundled = trustManager(store)
        CompositeTrustManager(system, bundled)
    }

    private fun trustManager(store: KeyStore?): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(store)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().single()
    }
}

private class CompositeTrustManager(
    private val system: X509TrustManager,
    private val bundled: X509TrustManager,
) : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
        system.checkClientTrusted(chain, authType)

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        try {
            system.checkServerTrusted(chain, authType)
        } catch (systemError: CertificateException) {
            try {
                bundled.checkServerTrusted(chain, authType)
            } catch (bundledError: CertificateException) {
                bundledError.addSuppressed(systemError)
                throw bundledError
            }
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = system.acceptedIssuers + bundled.acceptedIssuers
}
