package io.github.areiljan.cmrsign.cades

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.Activation
import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.csc.CscSigningProvider
import io.github.areiljan.cmrsign.pades.PadesSigner
import io.github.areiljan.cmrsign.pades.PadesValidator
import io.github.areiljan.cmrsign.unsignedPdf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.time.Instant
import java.util.Base64

@EnabledIfEnvironmentVariable(named = "CSC_BASE_URL", matches = ".+")
class CscProbeTest {
    private val provider = CscSigningProvider(
        baseUrl = env("CSC_BASE_URL"),
        tokenUrl = env("CSC_TOKEN_URL"),
        clientId = env("CSC_CLIENT_ID"),
        username = env("CSC_USERNAME"),
        password = env("CSC_PASSWORD"),
        credentialId = env("CSC_CREDENTIAL_ID"),
    )

    @Test
    fun `a real csc provider signs a pdf that DSS accepts`() {
        val started = provider.startSession("probe")
        val session = started.id
        if (started.activation == Activation.OTP) provider.authorize(session, otp())
        val identity = provider.completeSession(session)

        val signed = PadesSigner.forProvider(provider, session, identity).sign(unsignedPdf(), identity.subjectName)
        File("build/csc-probe-signed.pdf").writeBytes(signed)

        val verdict = PadesValidator(DssCmsValidator(listOf(identity.certificateChain.last())))
            .validate(DocumentToValidate(signed, MediaType.PDF))
        File("build/csc-probe-report.xml").writeText(verdict.report)
        println("CSC probe (${started.activation}): ${identity.subjectName} -> ${verdict.indication} ${verdict.subIndication} ${verdict.qualification}")

        expect(verdict.indication).toEqual(Indication.TOTAL_PASSED)
    }

    private fun otp() = System.getenv("CSC_OTP") ?: totp(Base64.getDecoder().decode(env("CSC_TOTP_SECRET")), Instant.now())

    private fun env(name: String) = requireNotNull(System.getenv(name)) { "$name is not set" }
}
