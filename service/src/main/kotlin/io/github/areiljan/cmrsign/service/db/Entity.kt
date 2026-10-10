package io.github.areiljan.cmrsign.service.db

import klite.Converter
import klite.jdbc.BaseCrudRepository
import klite.jdbc.BaseEntity
import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter.BASIC_ISO_DATE
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

@JvmInline
value class Id<T>(val value: String) {
    companion object {
        private var random = SecureRandom()
        private var counter = AtomicInteger()
        init { Converter.use { Id<Any>(it) } }
    }

    constructor(prefix: Char? = null): this(
        (prefix?.let { "$it-" } ?: "") +
                LocalDate.now().format(BASIC_ISO_DATE) +
                counter.addAndGet(random.nextInt(100) + 1).toString(36).uppercase().padStart(5, '0')
    )

    override fun toString() = value
}

typealias Entity<T> = BaseEntity<Id<T>>

abstract class CrudRepository<T: Entity<T>>(db: DataSource, table: String): BaseCrudRepository<T, Id<T>>(db, table) {
    override val orderAsc get() = "order by id"
}
