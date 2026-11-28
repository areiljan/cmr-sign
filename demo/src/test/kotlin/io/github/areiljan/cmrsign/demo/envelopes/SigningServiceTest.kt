package io.github.areiljan.cmrsign.demo.envelopes

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.Activation
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.ProviderSession
import io.github.areiljan.cmrsign.SessionStatus
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.SigningProvider
import io.github.areiljan.cmrsign.Validator
import io.github.areiljan.cmrsign.demo.db.BaseMocks
import io.github.areiljan.cmrsign.demo.reference.ReferenceSigningProvider
import io.github.areiljan.cmrsign.efti.EftiDataElement
import io.mockk.every
import io.mockk.slot
import klite.require
import org.junit.jupiter.api.Test

class SigningServiceTest : BaseMocks() {
    private val provider = OtpProvider()
    private val service = SigningService(envelopes, documents, reports, provider, registry.require<Validator>())
    private val unsigned = requireNotNull(javaClass.getResourceAsStream("/example-unsigned.pdf")).use { it.readBytes() }
    private val saved = slot<Envelope>()

    private fun envelope(signerName: String? = "Mari Maasikas") =
        Envelope(SignatureFormat.PADES, SignerClass.REGISTERED, EftiDataElement.EFTI65_SENDER, signerName = signerName).also {
            every { documents.by(Document::version to 1, Document::envelopeId to it.id) } returns Document(it.id, 1, MediaType.PDF, Sha256.of(unsigned), unsigned)
            every { envelopes.save(capture(saved)) } returns 1
        }

    @Test
    fun `a provider that needs an OTP leaves the envelope waiting for authorization`() {
        val started = service.start(envelope())

        expect(started.state).toEqual(EnvelopeState.AUTHORIZATION_PENDING)
        expect(started.activation).toEqual(Activation.OTP)
        expect(started.signerSubject).toEqual(null)
        expect(saved.captured.state).toEqual(EnvelopeState.AUTHORIZATION_PENDING)
    }

    @Test
    fun `cannot sign before the OTP is given`() {
        service.start(envelope())

        expect { service.sign(saved.captured, Sha256.of(unsigned)) }.toThrow<IllegalStateException>()
    }

    @Test
    fun `the OTP authorizes the session and the envelope is signed`() {
        service.start(envelope())
        val authorized = service.authorize(saved.captured, "123456")

        expect(authorized.state).toEqual(EnvelopeState.IDENTITY_PROVEN)
        expect(authorized.signerSubject).toEqual("CN=Mari Maasikas")

        expect(service.sign(saved.captured, Sha256.of(unsigned)).state).toEqual(EnvelopeState.VALIDATED)
    }

    @Test
    fun `a wrong OTP does not prove identity`() {
        service.start(envelope())

        expect { service.authorize(saved.captured, "000000") }.toThrow<IllegalStateException>()
    }

    @Test
    fun `an authorized envelope is not authorized again`() {
        service.start(envelope())
        service.authorize(saved.captured, "123456")

        expect { service.authorize(saved.captured, "123456") }.toThrow<IllegalStateException>()
    }

    @Test
    fun `refuses a certificate that names someone else`() {
        val other = SigningService(envelopes, documents, reports, NamingProvider("Jaan Tamm"), registry.require<Validator>())

        expect { other.start(envelope("Mari Maasikas")) }.toThrow<IllegalStateException>()
    }

    @Test
    fun `an envelope without an expected signer accepts the provider identity`() {
        val other = SigningService(envelopes, documents, reports, NamingProvider("Jaan Tamm"), registry.require<Validator>())

        expect(other.start(envelope(signerName = null)).signerSubject).toEqual("CN=Jaan Tamm")
    }

    private class OtpProvider(private val delegate: ReferenceSigningProvider = ReferenceSigningProvider()) : SigningProvider by delegate {
        private val authorized = mutableSetOf<String>()

        override fun startSession(signer: String) = delegate.startSession(signer).copy(status = SessionStatus.PENDING, activation = Activation.OTP)

        override fun session(session: String): ProviderSession =
            delegate.session(session).copy(status = if (session in authorized) SessionStatus.AUTHORIZED else SessionStatus.PENDING, activation = Activation.OTP)

        override fun authorize(session: String, activationCode: String): ProviderSession {
            if (activationCode == "123456") authorized += session
            return session(session)
        }

        override fun completeSession(session: String) = delegate.completeSession(session).also { check(session in authorized) }

        override fun sign(session: String, dataToSign: ByteArray) = delegate.sign(session, dataToSign).also { check(session in authorized) }
    }

    private class NamingProvider(private val name: String, private val delegate: ReferenceSigningProvider = ReferenceSigningProvider()) : SigningProvider by delegate {
        override fun startSession(signer: String) = delegate.startSession(name)
    }
}
