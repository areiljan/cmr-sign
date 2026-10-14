package io.github.areiljan.cmrsign.service.envelopes

import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
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
