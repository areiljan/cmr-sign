package io.github.areiljan.cmrsign

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

class PadesSignerTest {
    @Test
    fun `signs a pdf`() {
        val keyPair = keyPair()
        val certificate = certificate(keyPair)
        val unsigned = unsignedPdf()

        val signed = PadesSigner(keyPair.private, listOf(certificate)).sign(unsigned, "Test Signer")

        expect(signed.size > unsigned.size)
        Loader.loadPDF(signed).use { document ->
            expect(document.signatureDictionaries.size).toEqual(1)
            expect(document.signatureDictionaries.first().subFilter).toEqual(PDSignature.SUBFILTER_ETSI_CADES_DETACHED.name)
        }
    }

    private fun keyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048) }
            .generateKeyPair()

    private fun certificate(keyPair: KeyPair): X509Certificate {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        val subject = X500Name("CN=CMR Sign Test Signer")
        val now = System.currentTimeMillis()
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(now),
            Date(now),
            Date(now + TEN_YEARS_MILLIS),
            subject,
            keyPair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private)

        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(builder.build(signer))
    }

    private fun unsignedPdf(): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/example-unsigned.pdf")) {
            "missing test resource: example-unsigned.pdf"
        }.use { it.readBytes() }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        const val TEN_YEARS_MILLIS = 3650L * DAY_MILLIS
    }
}
