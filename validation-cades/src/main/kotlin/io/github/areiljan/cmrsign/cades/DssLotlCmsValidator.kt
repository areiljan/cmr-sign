package io.github.areiljan.cmrsign.cades

import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource
import eu.europa.esig.dss.tsl.job.TLValidationJob
import eu.europa.esig.dss.tsl.source.LOTLSource
import io.github.areiljan.cmrsign.CmsValidator
import io.github.areiljan.cmrsign.Verdict

/**
 * Validates against the EU list of trusted lists. The online refresh happens in
 * the constructor, so create one instance at startup and reuse it.
 */
class DssLotlCmsValidator @JvmOverloads constructor(
    lotlUrl: String = EU_LOTL_URL,
) : CmsValidator {
    private val trustSource: Any = loadTrustedLists(lotlUrl)

    override fun validate(cms: ByteArray, signedContent: ByteArray): Verdict =
        validateCms(trustSource as eu.europa.esig.dss.spi.x509.CertificateSource, cms, signedContent)

    companion object {
        const val EU_LOTL_URL = "https://ec.europa.eu/tools/lotl/eu-lotl.xml"

        private fun loadTrustedLists(lotlUrl: String): TrustedListsCertificateSource {
            val trustedLists = TrustedListsCertificateSource()
            TLValidationJob().apply {
                setTrustedListCertificateSource(trustedLists)
                setListOfTrustedListSources(LOTLSource().apply { url = lotlUrl })
                onlineRefresh()
            }
            return trustedLists
        }
    }
}
