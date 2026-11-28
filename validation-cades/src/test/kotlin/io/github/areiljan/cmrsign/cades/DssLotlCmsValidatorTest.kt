package io.github.areiljan.cmrsign.cades

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class DssLotlCmsValidatorTest {
    @Test
    fun `bundles the Official Journal LOTL signing certificates`() {
        expect(DssLotlCmsValidator.ojKeystore().certificates.size).toEqual(6)
    }
}
