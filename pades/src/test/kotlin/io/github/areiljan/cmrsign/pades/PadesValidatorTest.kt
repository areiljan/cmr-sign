package io.github.areiljan.cmrsign.pades

import ch.tutteli.atrium.api.fluent.en_GB.toBeTheInstance
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.CmsValidator
import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Qualification
import io.github.areiljan.cmrsign.SignerIdentity
import io.github.areiljan.cmrsign.Verdict
import io.github.areiljan.cmrsign.certificate
import io.github.areiljan.cmrsign.keyPair
import io.github.areiljan.cmrsign.unsignedPdf
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.ByteArrayOutputStream
import org.junit.jupiter.api.Test

class PadesValidatorTest {
    private val keyPair = keyPair()
    private val signed = PadesSigner(keyPair.private, listOf(certificate(keyPair))).sign(unsignedPdf(), "Test Signer")
    private val passed = Verdict.from(Indication.TOTAL_PASSED, null, Qualification.NA, SignerIdentity("Test Signer"), "report")
    private val validator = PadesValidator(CmsValidator { _, _ -> passed })

    @Test
    fun `intact pdf keeps the cms verdict`() {
        expect(validator.validate(DocumentToValidate(signed, MediaType.PDF))).toBeTheInstance(passed)
    }

    @Test
    fun `unsigned incremental update fails even when the cms passes`() {
        val updated = ByteArrayOutputStream()
        PDDocument.load(signed).use {
            it.documentInformation.title = "changed after signing"
            it.documentInformation.cosObject.isNeedToBeUpdated = true
            it.saveIncremental(updated)
        }

        val verdict = validator.validate(DocumentToValidate(updated.toByteArray(), MediaType.PDF))

        expect(verdict.indication).toEqual(Indication.TOTAL_FAILED)
        expect(verdict.subIndication).toEqual(PadesValidator.FORMAT_FAILURE)
    }
}
