package io.github.areiljan.cmrsign.demo.reference

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.cades.DssCmsValidator
import io.github.areiljan.cmrsign.pades.PadesSigner
import io.github.areiljan.cmrsign.pades.PadesValidator
import org.junit.jupiter.api.Test

class ReferenceSigningRoundTripTest {
    private val provider = ReferenceSigningProvider()
    private val unsigned = requireNotNull(javaClass.getResourceAsStream("/example-unsigned.pdf")).use { it.readBytes() }

    @Test
    fun `a per-signer reference certificate validates in DSS`() {
        val session = provider.startSession("Mari Maasikas").id
        val identity = provider.completeSession(session)

        val signed = PadesSigner.forProvider(provider, session, identity).sign(unsigned, identity.subjectName)
        val verdict = PadesValidator(DssCmsValidator(provider.trustAnchors)).validate(signed)

        expect(verdict.indication).toEqual(Indication.TOTAL_PASSED)
        expect(verdict.signer.isBoundTo("Mari Maasikas")).toEqual(true)
    }
}
