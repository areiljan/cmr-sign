package io.github.areiljan.cmrsign.service.db

import io.github.areiljan.cmrsign.Id
import klite.jdbc.BaseCrudRepository
import klite.jdbc.BaseEntity
import javax.sql.DataSource

typealias Entity<T> = BaseEntity<Id<T>>

abstract class CrudRepository<E: Entity<D>, D>(db: DataSource, table: String): BaseCrudRepository<E, Id<D>>(db, table) {
    override val orderAsc get() = "order by id"
}
