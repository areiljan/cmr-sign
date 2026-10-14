package io.github.areiljan.cmrsign

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class DocumentToValidateTest {
    @Test
    fun `keeps the bytes and content type`() {
        val bytes = byteArrayOf(1, 2, 3)

        val document = DocumentToValidate(bytes, MediaType.PDF)

        assertEquals(bytes, document.bytes)
        assertEquals(MediaType.PDF, document.contentType)
    }

    @Test
    fun `rejects an empty document`() {
        assertThrows<IllegalArgumentException> { DocumentToValidate(byteArrayOf(), MediaType.PDF) }
    }
}
