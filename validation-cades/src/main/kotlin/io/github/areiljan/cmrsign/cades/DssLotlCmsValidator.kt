package io.github.areiljan.cmrsign.cades

import eu.europa.esig.dss.service.http.commons.CommonsDataLoader
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource
import eu.europa.esig.dss.spi.x509.CertificateSource
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI
import eu.europa.esig.dss.tsl.job.TLValidationJob
import eu.europa.esig.dss.tsl.source.LOTLSource
import io.github.areiljan.cmrsign.CmsValidator
import io.github.areiljan.cmrsign.Verdict
import java.io.File

/**
 * Validates against the EU list of trusted lists. The LOTL signature is checked against the
 * signing certificates published in the Official Journal (bundled `eu-lotl-oj-keystore.p12`),
 * and pivots announced at [ojUrl] are followed. The online refresh happens in the constructor,
 * so create one instance at startup and reuse it.
 */
class DssLotlCmsValidator @JvmOverloads constructor(
    lotlUrl: String = EU_LOTL_URL,
    ojUrl: String = CURRENT_OJ_URL,
    cacheDirectory: File = File(System.getProperty("java.io.tmpdir"), "cmr-sign-tsl-cache"),
) : CmsValidator {
    private val trustSource: Any = loadTrustedLists(lotlUrl, ojUrl, cacheDirectory)

    val trustedCertificateCount: Int get() = (trustSource as TrustedListsCertificateSource).numberOfCertificates

    override fun validate(cms: ByteArray, signedContent: ByteArray): Verdict =
        validateCms(trustSource as CertificateSource, cms, signedContent)

    companion object {
        const val EU_LOTL_URL = "https://ec.europa.eu/tools/lotl/eu-lotl.xml"
        const val CURRENT_OJ_URL = "https://eur-lex.europa.eu/eli/C/2026/1944/oj"
        private const val OJ_KEYSTORE = "/eu-lotl-oj-keystore.p12"
        private const val OJ_KEYSTORE_PASSWORD = "dss-password"

        internal fun ojKeystore() = KeyStoreCertificateSource(
            requireNotNull(DssLotlCmsValidator::class.java.getResourceAsStream(OJ_KEYSTORE)) { "missing $OJ_KEYSTORE" },
            "PKCS12",
            OJ_KEYSTORE_PASSWORD.toCharArray(),
        )

        private fun loadTrustedLists(lotlUrl: String, ojUrl: String, cacheDirectory: File): TrustedListsCertificateSource {
            val trustedLists = TrustedListsCertificateSource()
            TLValidationJob().apply {
                setTrustedListCertificateSource(trustedLists)
                setListOfTrustedListSources(LOTLSource().apply {
                    url = lotlUrl
                    certificateSource = ojKeystore()
                    signingCertificatesAnnouncementPredicate = OfficialJournalSchemeInformationURI(ojUrl)
                    isPivotSupport = true
                })
                setOnlineDataLoader(FileCacheDataLoader(CommonsDataLoader()).apply {
                    setFileCacheDirectory(cacheDirectory)
                    setCacheExpirationTime(0)
                })
                onlineRefresh()
            }
            check(trustedLists.numberOfCertificates > 0) { "no trusted-list certificates loaded from $lotlUrl" }
            return trustedLists
        }
    }
}
