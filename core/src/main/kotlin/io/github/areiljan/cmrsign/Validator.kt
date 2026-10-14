package io.github.areiljan.cmrsign

fun interface Validator {
    fun validate(document: DocumentToValidate): Verdict
}
