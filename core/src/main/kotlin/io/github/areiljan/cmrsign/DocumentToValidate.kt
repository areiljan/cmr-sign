package io.github.areiljan.cmrsign

class DocumentToValidate(
    val bytes: ByteArray,
    val contentType: MediaType,
) {
    init {
        require(bytes.isNotEmpty()) { "document must not be empty" }
    }
}
