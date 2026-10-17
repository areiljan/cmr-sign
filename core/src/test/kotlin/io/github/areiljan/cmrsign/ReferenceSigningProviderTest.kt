package io.github.areiljan.cmrsign

import ch.tutteli.atrium.api.fluent.en_GB.notToEqual
import ch.tutteli.atrium.api.fluent.en_GB.toBeGreaterThan
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class ReferenceSigningProviderTest {
    private val provider = ReferenceSigningProvider()

    @Test
    fun `starts unique sessions`() {
        val first = provider.startSession("request-1")
        val second = provider.startSession("request-1")

        expect(first.length).toBeGreaterThan(0)
        expect(second.length).toBeGreaterThan(0)
        expect(first).notToEqual(second)
    }

    @Test
    fun `rejects a blank signer`() {
        expect { provider.startSession(" ") }.toThrow<IllegalArgumentException>()
    }

    @Test
    fun `completes a session with a reference identity`() {
        val session = provider.startSession("request-1")

        val identity = provider.completeSession(session)

        expect(identity.subjectName).toEqual("Reference Signer")
        expect(identity.proofingMethod).toEqual("reference-provider")
    }

    @Test
    fun `rejects an unknown session`() {
        expect { provider.completeSession("unknown") }.toThrow<IllegalArgumentException>()
    }

    @Test
    fun `rejects a completed session`() {
        val session = provider.startSession("request-1")
        provider.completeSession(session)

        expect { provider.completeSession(session) }.toThrow<IllegalArgumentException>()
    }
}
