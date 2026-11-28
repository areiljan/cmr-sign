package io.github.areiljan.cmrsign.demo.envelopes

import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.efti.EftiDataElement
import klite.HttpExchange
import klite.NotFoundException
import klite.NotModifiedException
import klite.StatusCode.Companion.OK
import klite.annotations.GET
import klite.annotations.POST
import klite.annotations.PathParam
import java.time.Duration
import java.time.Instant
import java.util.Base64

class EnvelopeRoutes(
    private val envelopes: EnvelopeRepository,
    private val documents: DocumentRepository,
    private val reports: ValidationReportRepository,
    private val links: SigningLinkRepository,
    private val signing: SigningService,
) {
    @POST
    fun create(body: EnvelopeCreationRequest): DomainEnvelope {
        val signerName = body.signerName?.trim()?.takeIf { it.isNotEmpty() }
        require(body.signerClass != SignerClass.AD_HOC || signerName != null) { "an ad hoc envelope needs a signer name" }
        val envelope = Envelope(format = body.format, signerClass = body.signerClass, eftiRole = body.eftiRole, signerName = signerName)
        envelopes.save(envelope)

        val payload = Base64.getDecoder().decode(body.payload)
        documents.save(
            Document(
                envelopeId = envelope.id,
                version = 1,
                contentType = body.contentType,
                sha256 = Sha256.of(payload),
                content = payload,
            )
        )
        return envelope.toDomain()
    }

    @GET("/:id")
    fun get(@PathParam id: Id<DomainEnvelope>): DomainEnvelope = envelopes.get(id).toDomain()

    @POST("/:id/start")
    fun start(@PathParam id: Id<DomainEnvelope>): SessionStarted {
        val envelope = envelopes.get(id)
        require(envelope.signerClass != SignerClass.AD_HOC) { "an ad hoc signer starts the session through a signing link" }
        return signing.start(envelope)
    }

    @POST("/:id/authorize")
    fun authorize(@PathParam id: Id<DomainEnvelope>, body: AuthorizeRequest): SessionStarted {
        val envelope = envelopes.get(id)
        require(envelope.signerClass != SignerClass.AD_HOC) { "an ad hoc signer authorizes through a signing link" }
        return signing.authorize(envelope, body.activationCode)
    }

    @POST("/:id/sign")
    fun sign(@PathParam id: Id<DomainEnvelope>, body: SignRequest): Signed {
        val envelope = envelopes.get(id)
        require(envelope.signerClass != SignerClass.AD_HOC) { "an ad hoc signer signs through a signing link" }
        return signing.sign(envelope, body.documentSha256)
    }

    @POST("/:id/links")
    fun createLink(@PathParam id: Id<DomainEnvelope>, body: LinkRequest): LinkIssued {
        val envelope = envelopes.get(id)
        require(envelope.signerClass == SignerClass.AD_HOC) { "only ad hoc envelopes get signing links" }
        check(envelope.state == EnvelopeState.CREATED) { "envelope is already ${envelope.state}" }
        require(body.ttlMinutes in 1..MAX_LINK_MINUTES) { "ttlMinutes must be between 1 and $MAX_LINK_MINUTES" }
        val token = SigningLinks.newToken()
        val otp = SigningLinks.newOtp()
        val link = SigningLink(id, Sha256.of(token.toByteArray()), Sha256.of(otp.toByteArray()), Instant.now().plus(Duration.ofMinutes(body.ttlMinutes)))
        links.save(link)
        return LinkIssued(id, token, otp, "/sign/$token", link.expiresAt)
    }

    @GET("/:id/documents/:version")
    fun download(exchange: HttpExchange, @PathParam id: Id<DomainEnvelope>, @PathParam version: Int) {
        val document = documents.by(Document::version to version, Document::envelopeId to id) ?: throw NotFoundException("no document $version for $id")
        exchange.sendDocument(document)
    }

    @GET("/:id/reports/:reportId")
    fun report(exchange: HttpExchange, @PathParam id: Id<DomainEnvelope>, @PathParam reportId: Id<ValidationReport>) {
        val report = reports.get(reportId)
        if (report.envelopeId != id) throw NotFoundException("no report $reportId for $id")
        exchange.send(OK, report.content, "application/xml")
    }

    private companion object {
        const val MAX_LINK_MINUTES = 7L * 24 * 60
    }
}

fun HttpExchange.sendDocument(document: Document) {
    val etag = "\"${document.sha256.hex}\""
    header("ETag", etag)
    if (header("If-None-Match") == etag) throw NotModifiedException()
    send(OK, document.content, document.contentType.value)
}

data class EnvelopeCreationRequest(
    val contentType: MediaType,
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val eftiRole: EftiDataElement,
    val payload: String, // base64
    val signerName: String? = null,
)

data class AuthorizeRequest(val activationCode: String)

data class SignRequest(val documentSha256: Sha256)

data class LinkRequest(val ttlMinutes: Long = 15)

data class LinkIssued(
    val id: Id<DomainEnvelope>,
    val token: String,
    val otp: String,
    val path: String,
    val expiresAt: Instant,
)
