package io.github.areiljan.cmrsign.service.envelopes

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.service.db.DBTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DocumentRepositoryTest : DBTest() {
    private val envelopes = EnvelopeRepository(db)
    private val documents = DocumentRepository(db)

    @Test
    fun `save and load`() {
        val envelope = Envelope(SignatureFormat.PADES, SignerClass.REGISTERED)
        envelopes.save(envelope)

        val content = "a small cmr payload".toByteArray()
        val document = Document(envelopeId = envelope.id, version = 12, contentType = MediaType.PDF, sha256 = Sha256.of(content), content = content)
        documents.save(document)

        val loaded = documents.get(document.id)
        expect(loaded.id).toEqual(document.id)
        expect(loaded.envelopeId).toEqual(document.envelopeId)
        expect(loaded.version).toEqual(document.version)
        expect(loaded.contentType).toEqual(document.contentType)
        expect(loaded.sha256).toEqual(document.sha256)
        assertTrue(loaded.content.contentEquals(document.content))
    }
}
