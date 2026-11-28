package io.github.areiljan.cmrsign.demo.envelopes

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.demo.db.BaseMocks
import io.github.areiljan.cmrsign.efti.EftiDataElement
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import klite.ForbiddenException
import klite.NotFoundException
import klite.StatusCode
import klite.StatusCodeException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant

class LinkRoutesTest : BaseMocks() {
    private val routes = create<LinkRoutes>()
    private val unsigned = requireNotNull(javaClass.getResourceAsStream("/example-unsigned.pdf")).use { it.readBytes() }
    private val envelope = Envelope(SignatureFormat.PADES, SignerClass.AD_HOC, EftiDataElement.EFTI84_CONSIGNEE, signerName = "Mari Maasikas")
    private val token = SigningLinks.newToken()
    private val otp = SigningLinks.newOtp()
    private val savedLink = slot<SigningLink>()
    private val savedEnvelope = slot<Envelope>()

    private fun link(
        expiresAt: Instant = Instant.now().plus(Duration.ofMinutes(10)),
        failedAttempts: Int = 0,
        verifiedAt: Instant? = null,
        usedAt: Instant? = null,
    ) = SigningLink(envelope.id, Sha256.of(token.toByteArray()), Sha256.of(otp.toByteArray()), expiresAt, failedAttempts, verifiedAt, usedAt)

    private fun given(link: SigningLink) {
        every { links.by(SigningLink::tokenHash to Sha256.of(token.toByteArray())) } returns link
    }

    @BeforeEach
    fun setUp() {
        every { envelopes.get(envelope.id) } returns envelope
        every { envelopes.save(capture(savedEnvelope)) } returns 1
        every { links.save(capture(savedLink)) } returns 1
        every { documents.by(Document::version to 1, Document::envelopeId to envelope.id) } returns Document(envelope.id, 1, MediaType.PDF, Sha256.of(unsigned), unsigned)
    }

    @Test
    fun `shows the document hash to the link holder`() {
        given(link())

        val view = routes.get(token)

        expect(view.signerName).toEqual("Mari Maasikas")
        expect(view.verified).toEqual(false)
        expect(view.documentSha256).toEqual(Sha256.of(unsigned))
    }

    @Test
    fun `an unknown token is not found`() {
        every { links.by(SigningLink::tokenHash to Sha256.of("unknown".toByteArray())) } returns null

        expect { routes.get("unknown") }.toThrow<NotFoundException>()
    }

    @Test
    fun `a wrong code counts an attempt`() {
        given(link(failedAttempts = 1))

        expect { routes.verify(token, OtpRequest("not-the-code")) }.toThrow<ForbiddenException>()

        expect(savedLink.captured.failedAttempts).toEqual(2)
        expect(savedLink.captured.verifiedAt).toEqual(null)
    }

    @Test
    fun `a link is locked after too many wrong codes`() {
        given(link(failedAttempts = SigningLinks.MAX_OTP_ATTEMPTS))

        expect { routes.verify(token, OtpRequest(otp)) }.toThrow<ForbiddenException>()
    }

    @Test
    fun `an expired link is rejected`() {
        given(link(expiresAt = Instant.now().minusSeconds(1)))

        expect(assertThrows<StatusCodeException> { routes.verify(token, OtpRequest(otp)) }.statusCode).toEqual(StatusCode.Gone)
    }

    @Test
    fun `a used link is rejected`() {
        given(link(verifiedAt = Instant.now(), usedAt = Instant.now()))

        expect(assertThrows<StatusCodeException> { routes.sign(token, SignRequest(Sha256.of(unsigned))) }.statusCode).toEqual(StatusCode.Gone)
    }

    @Test
    fun `cannot sign before the code is verified`() {
        given(link())

        expect { routes.sign(token, SignRequest(Sha256.of(unsigned))) }.toThrow<ForbiddenException>()
    }

    @Test
    fun `a link is verified only once`() {
        given(link(verifiedAt = Instant.now()))

        expect(assertThrows<StatusCodeException> { routes.verify(token, OtpRequest(otp)) }.statusCode).toEqual(StatusCode.Conflict)
    }

    @Test
    fun `the ad hoc signer verifies the code, signs and uses up the link`() {
        given(link())

        val started = routes.verify(token, OtpRequest(otp))

        expect(started.state).toEqual(EnvelopeState.IDENTITY_PROVEN)
        expect(started.signerSubject).toEqual("CN=Mari Maasikas")
        expect(started.proofingMethod).toEqual("platform-link-otp+reference-provider")
        expect(savedLink.captured.verifiedAt != null).toEqual(true)

        given(savedLink.captured)
        every { envelopes.get(envelope.id) } returns savedEnvelope.captured

        val signed = routes.sign(token, SignRequest(Sha256.of(unsigned)))

        expect(signed.state).toEqual(EnvelopeState.VALIDATED)
        expect(signed.verdict.indication).toEqual(Indication.TOTAL_PASSED)
        expect(signed.efti.dataElement).toEqual(EftiDataElement.EFTI84_CONSIGNEE)
        expect(savedLink.captured.usedAt != null).toEqual(true)

        given(savedLink.captured)
        expect { routes.get(token) }.toThrow<StatusCodeException>()
        verify(exactly = 1) { reports.save(any()) }
    }
}
