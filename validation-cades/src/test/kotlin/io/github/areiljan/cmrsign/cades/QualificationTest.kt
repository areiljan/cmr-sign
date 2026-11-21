package io.github.areiljan.cmrsign.cades

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import eu.europa.esig.dss.enumerations.SignatureQualification as DssQualification
import io.github.areiljan.cmrsign.Qualification
import org.junit.jupiter.api.Test

class QualificationTest {
    @Test
    fun `maps advanced and qualified DSS levels`() {
        val expected = mapOf(
            DssQualification.QESIG to Qualification.QESIG,
            DssQualification.QESEAL to Qualification.QESEAL,
            DssQualification.UNKNOWN_QC_QSCD to Qualification.QES,
            DssQualification.ADESIG to Qualification.ADESIG,
            DssQualification.ADESIG_QC to Qualification.ADESIG,
            DssQualification.ADESEAL to Qualification.ADESEAL,
            DssQualification.ADESEAL_QC to Qualification.ADESEAL,
            DssQualification.UNKNOWN to Qualification.ADES,
            DssQualification.UNKNOWN_QC to Qualification.ADES,
        )

        expect(expected.keys.associateWith(::qualificationOf)).toEqual(expected)
    }

    @Test
    fun `fails closed on indeterminate, not advanced and missing levels`() {
        val notAdvanced = DssQualification.entries.filter {
            it == DssQualification.NA || it.name.startsWith("INDETERMINATE_") || it.name.startsWith("NOT_ADES")
        }

        expect((notAdvanced + null).map(::qualificationOf).toSet()).toEqual(setOf(Qualification.NA))
    }
}
