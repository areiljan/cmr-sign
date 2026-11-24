package io.github.areiljan.cmrsign.demo.envelopes

import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.pades.PadesSigner
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.SigningProvider
import io.github.areiljan.cmrsign.Validator
import io.github.areiljan.cmrsign.Verdict
import io.github.areiljan.cmrsign.sign
import io.github.areiljan.cmrsign.startSession
import io.github.areiljan.cmrsign.validate
import io.github.areiljan.cmrsign.efti.EftiDataElement
import io.github.areiljan.cmrsign.efti.EftiSignatureStatus
import klite.HttpExchange
import klite.NotFoundException
import klite.NotModifiedException
import klite.StatusCode.Companion.OK
import klite.annotations.GET
import klite.annotations.POST
import klite.annotations.PathParam
import java.util.Base64

class EnvelopeRoutes(
    private val envelopes: EnvelopeRepository,
    private val documents: DocumentRepository,
    private val reports: ValidationReportRepository,
    private val signingProvider: SigningProvider,
    private val validator: Validator,
) {
    @POST
    fun create(body: EnvelopeCreationRequest): DomainEnvelope {
        val envelope = Envelope(format = body.format, signerClass = body.signerClass, eftiRole = body.eftiRole)
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
        require(envelope.signerClass == SignerClass.REGISTERED) { "only registered signers can start a session" }
        val document = documents.by(Document::version to 1, Document::envelopeId to id)
            ?: throw NotFoundException("no document for $id")
        val state = envelope.state.startSession()
        val sessionId = signingProvider.startSession(envelope.id.value)
        val identity = signingProvider.completeSession(sessionId)
        val updated = envelope.copy(state = state, providerSessionId = sessionId, signerSubject = identity.subjectName, proofingMethod = identity.proofingMethod,)
        envelopes.save(updated)
        return SessionStarted(id = updated.id, state = updated.state, providerSessionId = sessionId, signerSubject = identity.subjectName, proofingMethod = identity.proofingMethod, documentVersion = document.version, documentSha256 = document.sha256)
    }

    @POST("/:id/sign")
    fun sign(@PathParam id: Id<DomainEnvelope>, body: SignRequest): Signed {
        val envelope = envelopes.get(id)
        val sessionId = requireNotNull(envelope.providerSessionId) { "session not started" }
        val document = documents.by(Document::version to 1, Document::envelopeId to id)
            ?: throw NotFoundException("no document for $id")
        require(document.sha256 == body.documentSha256) { "document hash does not match" }
        val identity = signingProvider.completeSession(sessionId)
        val signedState = envelope.state.sign()
        val signed = PadesSigner.forProvider(signingProvider, sessionId, identity)
            .sign(document.content, identity.subjectName)
        val verdict = validator.validate(DocumentToValidate(signed, document.contentType))
        val version = document.version + 1
        val sha256 = Sha256.of(signed)
        documents.save(
            Document(
                envelopeId = id,
                version = version,
                contentType = document.contentType,
                sha256 = sha256,
                content = signed,
            )
        )
        val reportId = Id<ValidationReport>('R')
        val efti = EftiSignatureStatus.from(envelope.eftiRole, verdict, reportId.value)
        reports.save(ValidationReport(id, version, verdict.report, efti.signatureType, reportId))
        val updated = envelope.copy(state = signedState.validate())
        envelopes.save(updated)
        return Signed(updated.id, updated.state, version, sha256, verdict, efti)
    }

    @GET("/:id/documents/:version")
    fun download(exchange: HttpExchange, @PathParam id: Id<DomainEnvelope>, @PathParam version: Int) {
        val document = documents.by(Document::version to version, Document::envelopeId to id) ?: throw NotFoundException("no document $version for $id")
        val etag = "\"${document.sha256.hex}\""
        exchange.header("ETag", etag)
        if (exchange.header("If-None-Match") == etag) throw NotModifiedException()
        exchange.send(OK, document.content, document.contentType.value)
    }

    @GET("/:id/reports/:reportId")
    fun report(exchange: HttpExchange, @PathParam id: Id<DomainEnvelope>, @PathParam reportId: Id<ValidationReport>) {
        val report = reports.get(reportId)
        if (report.envelopeId != id) throw NotFoundException("no report $reportId for $id")
        exchange.send(OK, report.content, "application/xml")
    }
}

data class EnvelopeCreationRequest(
    val contentType: MediaType,
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val eftiRole: EftiDataElement,
    val payload: String, // base64
)

data class SessionStarted(
    val id: Id<DomainEnvelope>,
    val state: EnvelopeState,
    val providerSessionId: String,
    val signerSubject: String,
    val proofingMethod: String?,
    val documentVersion: Int,
    val documentSha256: Sha256,
)

data class SignRequest(
    val documentSha256: Sha256,
)

data class Signed(
    val id: Id<DomainEnvelope>,
    val state: EnvelopeState,
    val documentVersion: Int,
    val sha256: Sha256,
    val verdict: Verdict,
    val efti: EftiSignatureStatus,
)
