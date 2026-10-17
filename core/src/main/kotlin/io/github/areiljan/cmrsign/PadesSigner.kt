package io.github.areiljan.cmrsign

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.DERSet
import org.bouncycastle.asn1.cms.Attribute
import org.bouncycastle.asn1.cms.AttributeTable
import org.bouncycastle.asn1.ess.ESSCertIDv2
import org.bouncycastle.asn1.ess.SigningCertificateV2
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSAttributeTableGenerator
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.DigestCalculator
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Calendar

class PadesSigner private constructor(
    private val certificateChain: List<X509Certificate>,
    private val contentSigner: () -> ContentSigner,
) {
    constructor(privateKey: PrivateKey, certificateChain: List<X509Certificate>) : this(
        certificateChain,
        { JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(privateKey) },
    )

    init {
        require(certificateChain.isNotEmpty()) { "certificate chain must not be empty" }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun sign(pdf: ByteArray, signerName: String): ByteArray {
        val output = ByteArrayOutputStream()
        Loader.loadPDF(pdf).use { document ->
            val signature = signatureFor(signerName)
            document.addSignature(signature) { content -> buildCms(content.readBytes()) }
            document.saveIncremental(output)
        }
        return output.toByteArray()
    }

    private fun signatureFor(signerName: String) = PDSignature().apply {
        setFilter(PDSignature.FILTER_ADOBE_PPKLITE)
        setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED)
        name = signerName
        reason = "cmr-sign"
        signDate = Calendar.getInstance()
    }

    private fun buildCms(content: ByteArray): ByteArray {
        try {
            val signerCertificate = certificateChain.first()
            val generator = CMSSignedDataGenerator()
            generator.addSignerInfoGenerator(
                JcaSignerInfoGeneratorBuilder(digestCalculatorProvider())
                    .setSignedAttributeGenerator(signingCertificateAttributes(signerCertificate))
                    .build(contentSigner(), signerCertificate)
            )
            generator.addCertificates(JcaCertStore(certificateChain))
            return generator.generate(CMSProcessableByteArray(content), false).encoded
        } catch (e: Exception) {
            throw IOException("failed to build CMS signature", e)
        }
    }

    private fun digestCalculatorProvider() =
        JcaDigestCalculatorProviderBuilder()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build()

    private fun signingCertificateAttributes(certificate: X509Certificate): CMSAttributeTableGenerator {
        val digestCalculator: DigestCalculator = digestCalculatorProvider()
            .get(AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256))
        digestCalculator.outputStream.use { it.write(certificate.encoded) }

        val attribute = Attribute(
            PKCSObjectIdentifiers.id_aa_signingCertificateV2,
            DERSet(SigningCertificateV2(ESSCertIDv2(digestCalculator.digest))),
        )
        val vector = ASN1EncodableVector().apply { add(attribute) }
        return DefaultSignedAttributeTableGenerator(AttributeTable(vector))
    }

    private class ProviderContentSigner(
        private val provider: SigningProvider,
        private val session: String,
    ) : ContentSigner {
        private val buffer = ByteArrayOutputStream()

        override fun getOutputStream() = buffer

        override fun getAlgorithmIdentifier() =
            AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption, DERNull.INSTANCE)

        override fun getSignature(): ByteArray = provider.sign(session, buffer.toByteArray())
    }

    companion object {
        fun forProvider(provider: SigningProvider, session: String, identity: SignerIdentity): PadesSigner {
            require(identity.certificateChain.isNotEmpty()) { "signer identity must contain a certificate chain" }
            return PadesSigner(identity.certificateChain) { ProviderContentSigner(provider, session) }
        }
    }
}
