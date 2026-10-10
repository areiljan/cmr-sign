package io.github.areiljan.cmrsign

import java.security.MessageDigest

@JvmInline
value class Sha256(val hex: String) {
    init {
        require(hex.matches(SHA256_REGEX)) { "sha256 must be 64 lowercase hex characters: $hex" }
    }

    companion object {
        private val SHA256_REGEX = Regex("[0-9a-f]{64}")

        fun of(bytes: ByteArray): Sha256 {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return Sha256(digest.toHexString(HexFormat { upperCase = false }))
        }
    }
}
