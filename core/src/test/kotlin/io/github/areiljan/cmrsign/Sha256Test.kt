package io.github.areiljan.cmrsign

import ch.tutteli.atrium.api.fluent.en_GB.notToEqual
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class Sha256Test {
    private val valid = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    private val helloWorld = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"

    @Test
    fun `accepts a valid lowercase 64-character hex string`() {
        expect(Sha256(valid).hex).toEqual(valid)
    }

    @Test
    fun `rejects uppercase hex`() {
        expect { Sha256(valid.uppercase()) }.toThrow<IllegalArgumentException>()
    }

    @Test
    fun `rejects a value that is too short`() {
        expect { Sha256(valid.dropLast(1)) }.toThrow<IllegalArgumentException>()
    }

    @Test
    fun `rejects a value that is too long`() {
        expect { Sha256(valid + "0") }.toThrow<IllegalArgumentException>()
    }

    @Test
    fun `rejects non-hex characters`() {
        expect { Sha256("z".repeat(64)) }.toThrow<IllegalArgumentException>()
    }

    @Test
    fun `rejects an empty value`() {
        expect { Sha256("") }.toThrow<IllegalArgumentException>()
    }

    @Test
    fun `hash hello world`() {
        expect(Sha256.of("bye world".toByteArray()).hex).notToEqual(helloWorld)
        expect(Sha256.of("hello world".toByteArray()).hex).toEqual(helloWorld)
    }

    @Test
    fun `hash is deterministic`() {
        expect(Sha256.of("input".toByteArray()).hex).toEqual(Sha256.of("input".toByteArray()).hex)
    }
}
