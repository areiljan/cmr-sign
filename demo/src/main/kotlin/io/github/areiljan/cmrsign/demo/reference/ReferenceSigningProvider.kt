package io.github.areiljan.cmrsign.demo.reference

import io.github.areiljan.cmrsign.SignerIdentity
import io.github.areiljan.cmrsign.SigningProvider
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ReferenceSigningProvider : SigningProvider {
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()

    val certificate: X509Certificate = selfSignedCertificate(keyPair)

    private val sessions = ConcurrentHashMap<String, SignerIdentity>()

    override fun startSession(signer: String): String {
        require(signer.isNotBlank()) { "signer must not be blank" }
        val id = UUID.randomUUID().toString()
        sessions[id] = SignerIdentity(
            subjectName = certificate.subjectX500Principal.name,
            certificateChain = listOf(certificate),
            proofingMethod = "reference-provider",
        )
        return id
    }

    override fun completeSession(session: String): SignerIdentity =
        requireNotNull(sessions[session]) { "unknown session: $session" }

    override fun sign(session: String, dataToSign: ByteArray): ByteArray {
        require(sessions.containsKey(session)) { "unknown session: $session" }
        return Signature.getInstance("SHA256withRSA").run {
            initSign(keyPair.private)
            update(dataToSign)
            sign()
        }
    }

    private fun selfSignedCertificate(keyPair: KeyPair): X509Certificate {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        val subject = X500Name("CN=CMR Sign Reference Signer")
        val now = System.currentTimeMillis()
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(now),
            Date(now - DAY_MILLIS),
            Date(now + TEN_YEARS_MILLIS),
            subject,
            keyPair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private)

        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(builder.build(signer))
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        const val TEN_YEARS_MILLIS = 3650L * DAY_MILLIS
    }
}
