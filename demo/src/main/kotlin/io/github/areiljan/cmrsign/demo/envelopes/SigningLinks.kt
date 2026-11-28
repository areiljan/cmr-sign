package io.github.areiljan.cmrsign.demo.envelopes

import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.Sha256
import klite.ForbiddenException
import klite.HttpExchange
import klite.NotFoundException
import klite.StatusCode
import klite.StatusCodeException
import klite.annotations.GET
import klite.annotations.POST
import klite.annotations.PathParam
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

object SigningLinks {
    const val MAX_OTP_ATTEMPTS = 3
    private val random = SecureRandom()

    fun newToken(): String = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    fun newOtp(): String = random.nextInt(1_000_000).toString().padStart(6, '0')
}

class LinkRoutes(
    private val envelopes: EnvelopeRepository,
    private val links: SigningLinkRepository,
    private val signing: SigningService,
) {
    @GET("/:token")
    fun get(@PathParam token: String): LinkView {
        val link = open(token)
        val envelope = envelopes.get(link.envelopeId)
        val document = signing.original(envelope.id)
        return LinkView(envelope.id, envelope.signerName, envelope.state, link.verifiedAt != null, document.sha256, link.expiresAt)
    }

    @GET("/:token/document")
    fun document(exchange: HttpExchange, @PathParam token: String) {
        exchange.sendDocument(signing.original(open(token).envelopeId))
    }

    @POST("/:token/verify")
    fun verify(@PathParam token: String, body: OtpRequest): SessionStarted {
        val link = open(token)
        if (link.verifiedAt != null) throw StatusCodeException(StatusCode.Conflict, "link already verified")
        if (!MessageDigest.isEqual(link.otpHash.hex.toByteArray(), Sha256.of(body.otp.toByteArray()).hex.toByteArray())) {
            links.save(link.copy(failedAttempts = link.failedAttempts + 1))
            throw ForbiddenException("wrong code")
        }
        links.save(link.copy(verifiedAt = Instant.now()))
        return signing.start(envelopes.get(link.envelopeId), PROOFING)
    }

    @POST("/:token/authorize")
    fun authorize(@PathParam token: String, body: AuthorizeRequest): SessionStarted =
        signing.authorize(envelopes.get(verified(token).envelopeId), body.activationCode, PROOFING)

    @POST("/:token/sign")
    fun sign(@PathParam token: String, body: SignRequest): Signed {
        val link = verified(token)
        val signed = signing.sign(envelopes.get(link.envelopeId), body.documentSha256)
        links.save(link.copy(usedAt = Instant.now()))
        return signed
    }

    private fun verified(token: String) = open(token).also {
        if (it.verifiedAt == null) throw ForbiddenException("link not verified")
    }

    private fun open(token: String): SigningLink {
        val link = links.by(SigningLink::tokenHash to Sha256.of(token.toByteArray())) ?: throw NotFoundException("unknown link")
        when {
            link.usedAt != null -> throw StatusCodeException(StatusCode.Gone, "link already used")
            !Instant.now().isBefore(link.expiresAt) -> throw StatusCodeException(StatusCode.Gone, "link expired")
            link.failedAttempts >= SigningLinks.MAX_OTP_ATTEMPTS -> throw ForbiddenException("link locked")
        }
        return link
    }

    private companion object {
        const val PROOFING = "platform-link-otp"
    }
}

data class OtpRequest(val otp: String)

data class LinkView(
    val id: Id<DomainEnvelope>,
    val signerName: String?,
    val state: EnvelopeState,
    val verified: Boolean,
    val documentSha256: Sha256,
    val expiresAt: Instant,
)
