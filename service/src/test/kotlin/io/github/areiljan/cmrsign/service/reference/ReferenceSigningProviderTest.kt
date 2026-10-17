package io.github.areiljan.cmrsign.service.reference

import ch.tutteli.atrium.api.fluent.en_GB.notToEqual
import ch.tutteli.atrium.api.fluent.en_GB.toBeGreaterThan
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.security.Signature

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

        expect(identity.subjectName).toEqual(provider.certificate.subjectX500Principal.name)
        expect(identity.certificateChain).toEqual(listOf(provider.certificate))
        expect(identity.proofingMethod).toEqual("reference-provider")
    }

    @Test
    fun `completes a session repeatedly`() {
        val session = provider.startSession("request-1")

        expect(provider.completeSession(session)).toEqual(provider.completeSession(session))
    }

    @Test
    fun `signs the provided bytes`() {
        val session = provider.startSession("request-1")
        val data = "data to sign".toByteArray()

        val signature = provider.sign(session, data)
        val certificate = provider.certificate

        val verifier = Signature.getInstance("SHA256withRSA").apply {
            initVerify(certificate.publicKey)
            update(data)
        }
        expect(verifier.verify(signature)).toEqual(true)
    }

    @Test
    fun `rejects an unknown session`() {
        expect { provider.completeSession("unknown") }.toThrow<IllegalArgumentException>()
        expect { provider.sign("unknown", byteArrayOf(1)) }.toThrow<IllegalArgumentException>()
    }
}
