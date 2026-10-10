package io.github.areiljan.cmrsign

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class Sha256Test {
    private val valid = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    private val helloWorld = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"

    @Test
    fun `accepts a valid lowercase 64-character hex string`() {
        assertEquals(valid, Sha256(valid).hex)
    }

    @Test
    fun `rejects uppercase hex`() {
        assertThrows<IllegalArgumentException> { Sha256(valid.uppercase()) }
    }

    @Test
    fun `rejects a value that is too short`() {
        assertThrows<IllegalArgumentException> { Sha256(valid.dropLast(1)) }
    }

    @Test
    fun `rejects a value that is too long`() {
        assertThrows<IllegalArgumentException> { Sha256(valid + "0") }
    }

    @Test
    fun `rejects non-hex characters`() {
        assertThrows<IllegalArgumentException> { Sha256("z".repeat(64)) }
    }

    @Test
    fun `rejects an empty value`() {
        assertThrows<IllegalArgumentException> { Sha256("") }
    }

    @Test
    fun `hash hello world`() {
        assertNotEquals(helloWorld , Sha256.of("bye world".toByteArray()).hex)
        assertEquals(helloWorld , Sha256.of("hello world".toByteArray()).hex)
    }

    @Test
    fun `hash is deterministic`() {
        assertEquals(Sha256.of("input".toByteArray()).hex, Sha256.of("input".toByteArray()).hex)
    }
}
