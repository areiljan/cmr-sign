package io.github.areiljan.cmrsign.pades

import io.github.areiljan.cmrsign.CmsValidator
import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.Validator
import io.github.areiljan.cmrsign.Verdict
import org.apache.pdfbox.pdmodel.PDDocument
import org.bouncycastle.asn1.ASN1InputStream

class PadesValidator(private val cmsValidator: CmsValidator) : Validator {
    override fun validate(document: DocumentToValidate): Verdict = validate(document.bytes)

    fun validate(pdf: ByteArray): Verdict =
        PDDocument.load(pdf).use {
            val signature = requireNotNull(it.lastSignatureDictionary) { "no signature in the document" }
            val cms = ASN1InputStream(signature.getContents(pdf)).use { asn1 -> asn1.readObject().encoded }
            val verdict = cmsValidator.validate(cms, signature.getSignedContent(pdf))
            if (coversWholeFile(pdf, signature.byteRange)) verdict
            else Verdict.from(Indication.TOTAL_FAILED, FORMAT_FAILURE, verdict.qualification, verdict.signer, verdict.report)
        }

    private fun coversWholeFile(pdf: ByteArray, byteRange: IntArray): Boolean {
        if (byteRange.size != 4) return false
        val (start, gapStart, gapEnd, tailLength) = byteRange
        if (start != 0 || gapStart !in 1 until gapEnd || gapEnd.toLong() + tailLength != pdf.size.toLong()) return false
        return HEX_STRING.matches(String(pdf, gapStart, gapEnd - gapStart, Charsets.ISO_8859_1))
    }

    companion object {
        const val FORMAT_FAILURE = "FORMAT_FAILURE"
        private val HEX_STRING = Regex("<[0-9A-Fa-f]*>")
    }
}
