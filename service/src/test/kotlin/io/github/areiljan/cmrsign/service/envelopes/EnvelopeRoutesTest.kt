package io.github.areiljan.cmrsign.service.envelopes

import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.service.db.BaseMocks
import io.mockk.every
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
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

        assertEquals(created, envelopeSlot.captured)
        assertEquals(Envelope.State.CREATED, envelopeSlot.captured.state)

        with(documentSlot.captured) {
            assertEquals(created.id, envelopeId)
            assertEquals(1, version)
            assertEquals(MediaType.PDF, contentType)
            assertEquals(Sha256.of(payload), sha256)
            assertTrue(payload.contentEquals(content))
        }
    }
}
