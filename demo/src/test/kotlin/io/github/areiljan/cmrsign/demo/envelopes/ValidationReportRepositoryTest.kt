package io.github.areiljan.cmrsign.demo.envelopes

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.efti.EftiDataElement
import io.github.areiljan.cmrsign.efti.EftiSignatureType
import io.github.areiljan.cmrsign.demo.db.DBTest
import org.junit.jupiter.api.Test

class ValidationReportRepositoryTest : DBTest() {
    private val envelopes = EnvelopeRepository(db)
    private val reports = ValidationReportRepository(db)

    @Test
    fun `save and load`() {
        val envelope = Envelope(SignatureFormat.PADES, SignerClass.REGISTERED, EftiDataElement.EFTI65_SENDER)
        envelopes.save(envelope)
        val report = ValidationReport(envelope.id, 2, "<ValidationReport/>", EftiSignatureType.ADES)

        reports.save(report)

        expect(reports.get(report.id)).toEqual(report)
    }
}
