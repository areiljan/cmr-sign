package io.github.areiljan.cmrsign.service.envelopes

import io.github.areiljan.cmrsign.service.db.CrudRepository
import javax.sql.DataSource

class EnvelopeRepository(db: DataSource): CrudRepository<Envelope>(db, "envelopes")

class DocumentRepository(db: DataSource): CrudRepository<Document>(db, "documents")
