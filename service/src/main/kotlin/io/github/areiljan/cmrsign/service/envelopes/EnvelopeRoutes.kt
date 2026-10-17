package io.github.areiljan.cmrsign.service.envelopes

import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.SigningProvider
import io.github.areiljan.cmrsign.startSession
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
    private val signingProvider: SigningProvider,
) {
    @POST
    fun create(body: EnvelopeCreationRequest): DomainEnvelope {
        val envelope = Envelope(format = body.format, signerClass = body.signerClass)
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
        val state = envelope.state.startSession()
        val sessionId = signingProvider.startSession(envelope.id.value)
        val identity = signingProvider.completeSession(sessionId)
        val updated = envelope.copy(state = state, providerSessionId = sessionId, signerSubject = identity.subjectName, proofingMethod = identity.proofingMethod,)
        envelopes.save(updated)
        return SessionStarted(id = updated.id, state = updated.state, providerSessionId = sessionId, signerSubject = identity.subjectName, proofingMethod = identity.proofingMethod,)
    }

    @GET("/:id/documents/:version")
    fun download(exchange: HttpExchange, @PathParam id: Id<DomainEnvelope>, @PathParam version: Int) {
        val document = documents.by(Document::version to version, Document::envelopeId to id) ?: throw NotFoundException("no document $version for $id")
        val etag = "\"${document.sha256.hex}\""
        exchange.header("ETag", etag)
        if (exchange.header("If-None-Match") == etag) throw NotModifiedException()
        exchange.send(OK, document.content, document.contentType.value)
    }
}

data class EnvelopeCreationRequest(
    val contentType: MediaType,
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val payload: String, // base64
)

data class SessionStarted(
    val id: Id<DomainEnvelope>,
    val state: EnvelopeState,
    val providerSessionId: String,
    val signerSubject: String,
    val proofingMethod: String?,
)
