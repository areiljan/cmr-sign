package io.github.areiljan.cmrsign.demo.reference

import io.github.areiljan.cmrsign.ProviderSession
import io.github.areiljan.cmrsign.SessionStatus
import io.github.areiljan.cmrsign.SignerIdentity
import io.github.areiljan.cmrsign.SigningProvider
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Development provider: a self-signed root issues one certificate per signer, so the certificate
 * names the signer like a real provider would. The issued certificates are offered as [trustAnchors]
 * because there is no CRL or OCSP responder behind them. Keys live in memory; nothing here is a legal signature.
 */
class ReferenceSigningProvider : SigningProvider {
    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(BouncyCastleProvider())
    }

    private val rootKeys = keyPair()
    private val serials = AtomicLong(System.currentTimeMillis())

    val root: X509Certificate = certificate(ROOT_NAME, rootKeys.public, ROOT_NAME, rootKeys.private, ca = true)

    val trustAnchors: List<X509Certificate> = CopyOnWriteArrayList(listOf(root))

    private val sessions = ConcurrentHashMap<String, Session>()

    override fun startSession(signer: String): ProviderSession {
        require(signer.isNotBlank()) { "signer must not be blank" }
        val keys = keyPair()
        val leaf = certificate(signer.trim(), keys.public, ROOT_NAME, rootKeys.private, ca = false)
        (trustAnchors as MutableList).add(leaf)
        val id = UUID.randomUUID().toString()
        sessions[id] = Session(keys.private, SignerIdentity(leaf.subjectX500Principal.name, listOf(leaf, root), PROOFING_METHOD))
        return ProviderSession(id, SessionStatus.AUTHORIZED)
    }

    override fun session(session: String): ProviderSession {
        known(session)
        return ProviderSession(session, SessionStatus.AUTHORIZED)
    }

    override fun completeSession(session: String): SignerIdentity = known(session).identity

    override fun sign(session: String, dataToSign: ByteArray): ByteArray =
        Signature.getInstance("SHA256withRSA").run {
            initSign(known(session).key)
            update(dataToSign)
            sign()
        }

    private fun known(session: String) = requireNotNull(sessions[session]) { "unknown session: $session" }

    private fun certificate(subject: String, publicKey: PublicKey, issuer: String, issuerKey: PrivateKey, ca: Boolean): X509Certificate {
        val now = System.currentTimeMillis()
        val builder = JcaX509v3CertificateBuilder(
            name(issuer), BigInteger.valueOf(serials.incrementAndGet()),
            Date(now - DAY_MILLIS), Date(now + TEN_YEARS_MILLIS),
            name(subject), publicKey,
        )
            .addExtension(Extension.basicConstraints, true, BasicConstraints(ca))
            .addExtension(Extension.keyUsage, true, KeyUsage(if (ca) KeyUsage.keyCertSign or KeyUsage.cRLSign else KeyUsage.digitalSignature or KeyUsage.nonRepudiation))
        val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(issuerKey)
        return JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(builder.build(signer))
    }

    private fun name(commonName: String) = X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, commonName).build()

    private fun keyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private class Session(val key: PrivateKey, val identity: SignerIdentity)

    companion object {
        const val PROOFING_METHOD = "reference-provider"
        private const val ROOT_NAME = "CMR Sign Reference Root"
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
        private const val TEN_YEARS_MILLIS = 3650L * DAY_MILLIS
    }
}
