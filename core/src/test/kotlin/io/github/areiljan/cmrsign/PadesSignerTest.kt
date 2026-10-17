package io.github.areiljan.cmrsign

import ch.tutteli.atrium.api.fluent.en_GB.toBeGreaterThan
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.junit.jupiter.api.Test

class PadesSignerTest {
    @Test
    fun `sign pdf`() {
        val keyPair = keyPair()
        val certificate = certificate(keyPair)
        val unsigned = unsignedPdf()

        val signed = PadesSigner(keyPair.private, listOf(certificate)).sign(unsigned, "Test Signer")

        expect(signed.size).toBeGreaterThan(unsigned.size)
        Loader.loadPDF(signed).use { document ->
            expect(document.signatureDictionaries.size).toEqual(1)
            expect(document.signatureDictionaries.first().subFilter).toEqual(PDSignature.SUBFILTER_ETSI_CADES_DETACHED.name)
        }
    }
}
