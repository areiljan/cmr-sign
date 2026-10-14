package io.github.areiljan.cmrsign.service.envelopes

import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.service.db.BaseMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import klite.HttpExchange
import klite.NotFoundException
import klite.NotModifiedException
import klite.StatusCode.Companion.OK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

class EnvelopeRoutesTest : BaseMocks() {

    private val routes = create<EnvelopeRoutes>()

    @Test
    fun `creates an envelope and stores the payload as document version 1`() {
        val payload = "a small cmr payload".toByteArray()
        val envelopeSlot = slot<Envelope>()
        val documentSlot = slot<Document>()
        every { envelopes.save(capture(envelopeSlot)) } returns 1
        every { documents.save(capture(documentSlot)) } returns 1

        val created = routes.create(
            EnvelopeCreationRequest(
                contentType = MediaType.PDF,
                format = SignatureFormat.PADES,
                signerClass = SignerClass.REGISTERED,
                payload = Base64.getEncoder().encodeToString(payload),
            )
        )

        assertEquals(created, envelopeSlot.captured.toDomain())
        assertEquals(EnvelopeState.CREATED, envelopeSlot.captured.state)

        with(documentSlot.captured) {
            assertEquals(created.id, envelopeId)
            assertEquals(1, version)
            assertEquals(MediaType.PDF, contentType)
            assertEquals(Sha256.of(payload), sha256)
            assertTrue(payload.contentEquals(content))
        }
    }

    @Test
    fun `downloads the stored payload with its hash as ETag`() {
        val payload = "signed bytes".toByteArray()
        val envelopeId = Id<DomainEnvelope>()
        val document = Document(envelopeId, 1, MediaType.PDF, Sha256.of(payload), payload)
        every { documents.by(Document::version to 1, Document::envelopeId to envelopeId) } returns document
        val exchange = mockk<HttpExchange>(relaxed = true)
        every { exchange.header("If-None-Match") } returns null

        routes.download(exchange, envelopeId, 1)

        verify { exchange.header("ETag", "\"${document.sha256.hex}\"") }
        verify { exchange.send(OK, payload, MediaType.PDF.value) }
    }

    @Test
    fun `returns not modified when the ETag matches`() {
        val payload = "signed bytes".toByteArray()
        val envelopeId = Id<DomainEnvelope>()
        val document = Document(envelopeId, 1, MediaType.PDF, Sha256.of(payload), payload)
        every { documents.by(Document::version to 1, Document::envelopeId to envelopeId) } returns document
        val exchange = mockk<HttpExchange>(relaxed = true)
        every { exchange.header("If-None-Match") } returns "\"${document.sha256.hex}\""

        assertThrows(NotModifiedException::class.java) { routes.download(exchange, envelopeId, 1) }
    }

    @Test
    fun `reports a missing document as not found`() {
        val envelopeId = Id<DomainEnvelope>()
        every { documents.by(Document::version to 7, Document::envelopeId to envelopeId) } returns null
        val exchange = mockk<HttpExchange>(relaxed = true)

        assertThrows(NotFoundException::class.java) { routes.download(exchange, envelopeId, 7) }
    }
}
