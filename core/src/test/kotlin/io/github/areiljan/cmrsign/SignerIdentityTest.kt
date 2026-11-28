package io.github.areiljan.cmrsign

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class SignerIdentityTest {
    @Test
    fun `reads the common name from the subject`() {
        expect(SignerIdentity("CN=Mari Maasikas,O=Example,C=EE").commonName).toEqual("Mari Maasikas")
    }

    @Test
    fun `is bound to the expected signer regardless of case and spacing`() {
        expect(SignerIdentity("CN=Mari  Maasikas,C=EE").isBoundTo(" mari maasikas ")).toEqual(true)
    }

    @Test
    fun `is not bound to another signer`() {
        expect(SignerIdentity("CN=Mari Maasikas,C=EE").isBoundTo("Jaan Tamm")).toEqual(false)
    }

    @Test
    fun `a subject without a common name is never bound`() {
        expect(SignerIdentity("O=Example,C=EE").isBoundTo("Example")).toEqual(false)
        expect(SignerIdentity("not a distinguished name").isBoundTo("not a distinguished name")).toEqual(false)
    }

    @Test
    fun `defaults to a person`() {
        expect(SignerIdentity("CN=Mari Maasikas").kind).toEqual(SignerKind.PERSON)
    }
}
