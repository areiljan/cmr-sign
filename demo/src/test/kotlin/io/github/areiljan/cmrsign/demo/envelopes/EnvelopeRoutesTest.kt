package io.github.areiljan.cmrsign.demo.envelopes

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.demo.db.BaseMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import klite.HttpExchange
import klite.NotFoundException
import klite.NotModifiedException
import klite.StatusCode.Companion.OK
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

        expect(envelopeSlot.captured.toDomain()).toEqual(created)
        expect(envelopeSlot.captured.state).toEqual(EnvelopeState.CREATED)

        with(documentSlot.captured) {
            expect(envelopeId).toEqual(created.id)
            expect(version).toEqual(1)
            expect(contentType).toEqual(MediaType.PDF)
            expect(sha256).toEqual(Sha256.of(payload))
            expect(content.toList()).toEqual(payload.toList())
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

        expect { routes.download(exchange, envelopeId, 1) }.toThrow<NotModifiedException>()
    }

    @Test
    fun `reports a missing document as not found`() {
        val envelopeId = Id<DomainEnvelope>()
        every { documents.by(Document::version to 7, Document::envelopeId to envelopeId) } returns null
        val exchange = mockk<HttpExchange>(relaxed = true)

        expect { routes.download(exchange, envelopeId, 7) }.toThrow<NotFoundException>()
    }

    @Test
    fun `session flow`() {
        val envelope = Envelope(SignatureFormat.PADES, SignerClass.REGISTERED)
        every { envelopes.get(envelope.id) } returns envelope
        val document = Document(envelope.id, 1, MediaType.PDF, Sha256.of(unsignedPdf()), unsignedPdf())
        every { documents.by(Document::version to 1, Document::envelopeId to envelope.id) } returns document
        val saved = slot<Envelope>()
        every { envelopes.save(capture(saved)) } returns 1

        val started = routes.start(envelope.id)

        expect(started.state).toEqual(EnvelopeState.IDENTITY_PROVEN)
        expect(started.providerSessionId.isNotBlank()).toEqual(true)
        expect(started.signerSubject).toEqual(signingProvider.certificate.subjectX500Principal.name)
        expect(started.proofingMethod).toEqual("reference-provider")
        expect(started.documentVersion).toEqual(1)
        expect(started.documentSha256).toEqual(document.sha256)
        expect(saved.captured.state).toEqual(EnvelopeState.IDENTITY_PROVEN)
        expect(saved.captured.providerSessionId).toEqual(started.providerSessionId)
        expect(saved.captured.signerSubject).toEqual(signingProvider.certificate.subjectX500Principal.name)
        expect(saved.captured.proofingMethod).toEqual("reference-provider")
    }

    @Test
    fun `sign flow`() {
        val envelope = Envelope(SignatureFormat.PADES, SignerClass.REGISTERED)
        every { envelopes.get(envelope.id) } returns envelope
        val savedEnvelope = slot<Envelope>()
        every { envelopes.save(capture(savedEnvelope)) } returns 1

        val unsigned = unsignedPdf()
        val document = Document(envelope.id, 1, MediaType.PDF, Sha256.of(unsigned), unsigned)
        every { documents.by(Document::version to 1, Document::envelopeId to envelope.id) } returns document

        routes.start(envelope.id)
        every { envelopes.get(envelope.id) } returns savedEnvelope.captured

        expect {
            routes.sign(envelope.id, SignRequest(Sha256.of("wrong".toByteArray())))
        }.toThrow<IllegalArgumentException>()

        val savedDocument = slot<Document>()
        every { documents.save(capture(savedDocument)) } returns 1

        val signed = routes.sign(envelope.id, SignRequest(document.sha256))

        expect(signed.state).toEqual(EnvelopeState.VALIDATED)
        expect(signed.documentVersion).toEqual(2)
        expect(signed.verdict.indication).toEqual(Indication.TOTAL_PASSED)
        expect(savedDocument.captured.version).toEqual(2)
        expect(savedDocument.captured.sha256).toEqual(Sha256.of(savedDocument.captured.content))
        expect(savedEnvelope.captured.state).toEqual(EnvelopeState.VALIDATED)
    }

    private fun unsignedPdf(): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/example-unsigned.pdf")) {
            "missing test resource: example-unsigned.pdf"
        }.use { it.readBytes() }
}
