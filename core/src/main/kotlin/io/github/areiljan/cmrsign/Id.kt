package io.github.areiljan.cmrsign

import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter.BASIC_ISO_DATE
import java.util.concurrent.atomic.AtomicInteger

@JvmInline
value class Id<T>(val value: String) {
    constructor(prefix: Char? = null): this(
        (prefix?.let { "$it-" } ?: "") +
                LocalDate.now().format(BASIC_ISO_DATE) +
                counter.addAndGet(random.nextInt(100) + 1).toString(36).uppercase().padStart(5, '0')
    )

    override fun toString() = value

    companion object {
        private val random = SecureRandom()
        private val counter = AtomicInteger()
    }
}
