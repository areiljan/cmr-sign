package io.github.areiljan.cmrsign

@JvmInline
value class MediaType(val value: String) {
    init {
        require(value.matches(TYPE_SUBTYPE)) { "media type must look like type/subtype: $value" }
    }

    companion object {
        private val TYPE_SUBTYPE = Regex("[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+")

        val PDF = MediaType("application/pdf")
        val JSON = MediaType("application/json")
    }
}
