package io.github.areiljan.cmrsign

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValidatorTest {
    @Test
    fun `a fake validator is usable`() {
        val signer = SignerIdentity("Test Signer")
        val validator = Validator { Verdict.from(Indication.TOTAL_PASSED, Qualification.NA, signer) }

        val verdict = validator.validate(DocumentToValidate(byteArrayOf(1), MediaType.PDF))

        assertEquals(Indication.TOTAL_PASSED, verdict.indication)
        assertEquals(Qualification.NA, verdict.qualification)
        assertEquals(signer, verdict.signer)
    }
}
