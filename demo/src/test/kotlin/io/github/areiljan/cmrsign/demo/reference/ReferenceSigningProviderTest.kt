package io.github.areiljan.cmrsign.demo.reference

import ch.tutteli.atrium.api.fluent.en_GB.notToEqual
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.Activation
import io.github.areiljan.cmrsign.SessionStatus
import org.junit.jupiter.api.Test
import java.security.Signature

class ReferenceSigningProviderTest {
    private val provider = ReferenceSigningProvider()

    @Test
    fun `starts unique authorized sessions that need no activation`() {
        val first = provider.startSession("Mari Maasikas")
        val second = provider.startSession("Mari Maasikas")

        expect(first.id).notToEqual(second.id)
        expect(first.status).toEqual(SessionStatus.AUTHORIZED)
        expect(first.activation).toEqual(Activation.NONE)
        expect(provider.session(first.id).status).toEqual(SessionStatus.AUTHORIZED)
    }

    @Test
    fun `rejects a blank signer`() {
        expect { provider.startSession(" ") }.toThrow<IllegalArgumentException>()
    }

    @Test
    fun `issues a certificate that names the signer under the reference root`() {
        val identity = provider.completeSession(provider.startSession("Mari Maasikas").id)

        expect(identity.commonName).toEqual("Mari Maasikas")
        expect(identity.isBoundTo("Mari Maasikas")).toEqual(true)
        expect(identity.certificateChain.size).toEqual(2)
        expect(identity.certificateChain.last()).toEqual(provider.root)
        identity.certificateChain.first().verify(provider.root.publicKey)
        expect(identity.proofingMethod).toEqual("reference-provider")
    }

    @Test
    fun `offers the issued certificates as trust anchors`() {
        val leaf = provider.completeSession(provider.startSession("Mari Maasikas").id).certificateChain.first()

        expect(provider.trustAnchors.first()).toEqual(provider.root)
        expect(provider.trustAnchors.contains(leaf)).toEqual(true)
    }

    @Test
    fun `completes a session repeatedly`() {
        val session = provider.startSession("Mari Maasikas").id

        expect(provider.completeSession(session)).toEqual(provider.completeSession(session))
    }

    @Test
    fun `signs with the signer's own key`() {
        val session = provider.startSession("Mari Maasikas").id
        val data = "data to sign".toByteArray()

        val signature = provider.sign(session, data)
        val publicKey = provider.completeSession(session).certificateChain.first().publicKey

        val verifier = Signature.getInstance("SHA256withRSA").apply {
            initVerify(publicKey)
            update(data)
        }
        expect(verifier.verify(signature)).toEqual(true)
    }

    @Test
    fun `rejects an unknown session`() {
        expect { provider.session("unknown") }.toThrow<IllegalArgumentException>()
        expect { provider.completeSession("unknown") }.toThrow<IllegalArgumentException>()
        expect { provider.sign("unknown", byteArrayOf(1)) }.toThrow<IllegalArgumentException>()
    }

    @Test
    fun `takes no activation code`() {
        val session = provider.startSession("Mari Maasikas").id

        expect { provider.authorize(session, "123456") }.toThrow<UnsupportedOperationException>()
    }
}
