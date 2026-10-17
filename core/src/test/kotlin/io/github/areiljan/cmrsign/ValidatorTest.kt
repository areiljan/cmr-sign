package io.github.areiljan.cmrsign

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class ValidatorTest {
    @Test
    fun `a fake validator is usable`() {
        val signer = SignerIdentity("Test Signer")
        val validator = Validator { Verdict.from(Indication.TOTAL_PASSED, null, Qualification.NA, signer) }

        val verdict = validator.validate(DocumentToValidate(byteArrayOf(1), MediaType.PDF))

        expect(verdict.indication).toEqual(Indication.TOTAL_PASSED)
        expect(verdict.subIndication).toEqual(null)
        expect(verdict.qualification).toEqual(Qualification.NA)
        expect(verdict.signer).toEqual(signer)
    }
}
