package io.github.areiljan.cmrsign

fun interface CmsValidator {
    fun validate(cms: ByteArray, signedContent: ByteArray): Verdict
}
