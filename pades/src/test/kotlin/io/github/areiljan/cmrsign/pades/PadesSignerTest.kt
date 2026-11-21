package io.github.areiljan.cmrsign.pades

import ch.tutteli.atrium.api.fluent.en_GB.toBeGreaterThan
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.certificate
import io.github.areiljan.cmrsign.keyPair
import io.github.areiljan.cmrsign.unsignedPdf
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ess.SigningCertificateV2
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.cms.CMSSignedData
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class PadesSignerTest {
    @Test
    fun `sign pdf`() {
        val keyPair = keyPair()
        val certificate = certificate(keyPair)
        val unsigned = unsignedPdf()

        val signed = PadesSigner(keyPair.private, listOf(certificate)).sign(unsigned, "Test Signer")

        expect(signed.size).toBeGreaterThan(unsigned.size)
        PDDocument.load(signed).use { document ->
            expect(document.signatureDictionaries.size).toEqual(1)
            expect(document.signatureDictionaries.first().subFilter).toEqual(PDSignature.SUBFILTER_ETSI_CADES_DETACHED.name)
        }
    }

    @Test
    fun `signed attributes bind the signing certificate by its sha-256 hash`() {
        val keyPair = keyPair()
        val certificate = certificate(keyPair)

        val signed = PadesSigner(keyPair.private, listOf(certificate)).sign(unsignedPdf(), "Test Signer")

        val attribute = cmsOf(signed).signerInfos.signers.single().signedAttributes
            .get(PKCSObjectIdentifiers.id_aa_signingCertificateV2)
        val certId = SigningCertificateV2.getInstance(attribute.attrValues.getObjectAt(0)).certs.single()
        expect(certId.certHash.toList())
            .toEqual(MessageDigest.getInstance("SHA-256").digest(certificate.encoded).toList())
    }

    private fun cmsOf(pdf: ByteArray): CMSSignedData = PDDocument.load(pdf).use {
        val contents = it.lastSignatureDictionary.getContents(pdf)
        CMSSignedData(ASN1InputStream(contents).use { asn1 -> asn1.readObject().encoded })
    }
}
