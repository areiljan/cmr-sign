package io.github.areiljan.cmrsign.demo.envelopes

import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.demo.db.CrudRepository
import klite.jdbc.create
import klite.jdbc.toDBValues
import java.sql.ResultSet
import javax.sql.DataSource

class EnvelopeRepository(db: DataSource): CrudRepository<Envelope, DomainEnvelope>(db, "envelopes") {
    // TODO: think of an easier solution and get rid of the mapper and persister here
    override fun ResultSet.mapper() =
        create(Envelope::class, Envelope::state to envelopeState(getString("state")))

    override fun Envelope.persister() = toDBValues().mapValues { (prop, value) ->
        if (prop == Envelope::state) (value as EnvelopeState).dbValue else value
    }
}

class DocumentRepository(db: DataSource): CrudRepository<Document, Document>(db, "documents")

private val EnvelopeState.dbValue get() = name

private fun envelopeState(value: String) = EnvelopeState.valueOf(value)
