package io.github.areiljan.cmrsign.csc

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.areiljan.cmrsign.Activation
import io.github.areiljan.cmrsign.SessionStatus
import io.github.areiljan.cmrsign.SignerKind
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Security
import java.security.Signature
import java.security.cert.X509Certificate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CscSigningProviderTest {
    private lateinit var server: HttpServer
    private val requests = mutableListOf<Pair<String, String>>()
    private val authorizations = mutableListOf<String?>()

    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val certificate = selfSignedCertificate()

    private var credentialMode = """"authMode": "implicit""""
    private var authorizeStatus = 200
    private var now = Instant.parse("2026-11-28T10:00:00Z")
    private val clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = now
    }

    private lateinit var provider: CscSigningProvider

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/oauth2/token") { respond(it, """{"access_token": "token-1"}""") }
            createContext("/csc/v1/credentials/info") { respond(it, """{"cert": {"certificates": ["${certificateBase64()}"]}, $credentialMode}""") }
            createContext("/csc/v1/credentials/sendOTP") { respond(it, "") }
            createContext("/csc/v1/credentials/authorize") { respond(it, """{"SAD": "sad-123"}""", authorizeStatus) }
            createContext("/csc/v1/signatures/signHash") { respond(it, """{"signatures": ["${signatureBase64()}"]}""") }
            start()
        }
        provider = CscSigningProvider(
            baseUrl = "http://localhost:${server.address.port}/csc/v1",
            tokenUrl = "http://localhost:${server.address.port}/oauth2/token",
            clientId = "test-client",
            username = "signer@example.com",
            password = "secret",
            credentialId = "cred-1",
            kind = SignerKind.PERSON,
            sessionTtl = Duration.ofMinutes(5),
            clock = clock,
        )
    }

    @AfterEach
    fun stop() = server.stop(0)

    @Test
    fun `an implicit credential is authorized when the session starts`() {
        val session = provider.startSession("signer")

        assertEquals(SessionStatus.AUTHORIZED, session.status)
        assertEquals(Activation.NONE, session.activation)
        assertEquals(now.plus(Duration.ofMinutes(5)), session.expiresAt)
    }

    @Test
    fun `completeSession returns the signer identity from the credential certificate`() {
        val identity = provider.completeSession(provider.startSession("signer").id)

        assertEquals(certificate.subjectX500Principal.name, identity.subjectName)
        assertEquals(listOf(certificate), identity.certificateChain)
        assertEquals(SignerKind.PERSON, identity.kind)
    }

    @Test
    fun `sign authorizes the hash and then signs it`() {
        val hash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest("data".toByteArray()))
        val session = provider.startSession("signer").id
        requests.clear()

        val signature = provider.sign(session, "data".toByteArray())

        assertTrue(signature.isNotEmpty())
        assertEquals(listOf("/oauth2/token", "/csc/v1/credentials/authorize", "/csc/v1/signatures/signHash"), requests.map { it.first })
        assertTrue(requests[1].second.contains(hash))
        assertFalse(requests[1].second.contains("OTP"))
        assertTrue(requests[2].second.contains("\"SAD\":\"sad-123\""))
        assertTrue(requests[2].second.contains(hash))
        assertTrue(requests[2].second.contains("\"signAlgo\":\"1.2.840.113549.1.1.11\""))
    }

    @Test
    fun `calls the csc api with the oauth2 access token`() {
        provider.sign(provider.startSession("signer").id, "data".toByteArray())

        assertTrue(requests[0].second.contains("grant_type=password"))
        assertEquals(listOf(null, "Bearer token-1", null, "Bearer token-1", "Bearer token-1"), authorizations)
    }

    @Test
    fun `an explicit OTP credential waits for the code and sends it with the hash`() {
        credentialMode = """"authMode": "explicit", "OTP": {"presence": "true", "type": "offline"}"""

        val session = provider.startSession("signer")

        assertEquals(SessionStatus.PENDING, session.status)
        assertEquals(Activation.OTP, session.activation)
        assertFalse(requests.any { it.first.endsWith("/sendOTP") })
        assertThrows<IllegalStateException> { provider.completeSession(session.id) }
        assertThrows<IllegalStateException> { provider.sign(session.id, "data".toByteArray()) }

        assertEquals(SessionStatus.AUTHORIZED, provider.authorize(session.id, "123456").status)
        provider.sign(session.id, "data".toByteArray())

        assertTrue(requests.single { it.first.endsWith("/credentials/authorize") }.second.contains("\"OTP\":\"123456\""))
    }

    @Test
    fun `an online OTP is requested from the provider when the session starts`() {
        credentialMode = """"authMode": "explicit", "OTP": {"presence": "true", "type": "online"}"""

        provider.startSession("signer")

        assertEquals("""{"credentialID":"cred-1"}""", requests.single { it.first.endsWith("/credentials/sendOTP") }.second)
    }

    @Test
    fun `an OTP authorizes one signature only`() {
        credentialMode = """"authMode": "explicit", "OTP": {"presence": "true", "type": "offline"}"""
        val session = provider.startSession("signer").id
        provider.authorize(session, "123456")

        provider.sign(session, "data".toByteArray())
        provider.sign(session, "data".toByteArray())

        val authorizeBodies = requests.filter { it.first.endsWith("/credentials/authorize") }.map { it.second }
        assertTrue(authorizeBodies[0].contains("123456"))
        assertFalse(authorizeBodies[1].contains("123456"))
    }

    @Test
    fun `a rejected OTP fails the session`() {
        credentialMode = """"authMode": "explicit", "OTP": {"presence": "true", "type": "offline"}"""
        authorizeStatus = 400
        val session = provider.startSession("signer").id
        provider.authorize(session, "000000")

        assertThrows<IOException> { provider.sign(session, "data".toByteArray()) }

        assertEquals(SessionStatus.FAILED, provider.session(session).status)
        assertThrows<IllegalStateException> { provider.sign(session, "data".toByteArray()) }
    }

    @Test
    fun `an authorized session cannot be authorized again`() {
        val session = provider.startSession("signer").id

        assertThrows<IllegalStateException> { provider.authorize(session, "123456") }
    }

    @Test
    fun `an expired session cannot be authorized or signed`() {
        credentialMode = """"authMode": "explicit", "OTP": {"presence": "true", "type": "offline"}"""
        val pending = provider.startSession("signer").id
        val implicit = run {
            credentialMode = """"authMode": "implicit""""
            provider.startSession("signer").id
        }

        now = now.plus(Duration.ofMinutes(5))

        assertEquals(SessionStatus.EXPIRED, provider.session(pending).status)
        assertThrows<IllegalStateException> { provider.authorize(pending, "123456") }
        assertThrows<IllegalStateException> { provider.sign(implicit, "data".toByteArray()) }
    }

    @Test
    fun `PIN and oauth2code credentials are rejected`() {
        credentialMode = """"authMode": "explicit", "PIN": {"presence": "true"}"""
        assertThrows<UnsupportedOperationException> { provider.startSession("signer") }

        credentialMode = """"authMode": "oauth2code""""
        assertThrows<UnsupportedOperationException> { provider.startSession("signer") }
    }

    @Test
    fun `rejects an unknown session`() {
        assertThrows<IllegalArgumentException> { provider.session("unknown") }
        assertThrows<IllegalArgumentException> { provider.sign("unknown", byteArrayOf(1)) }
    }

    @Test
    fun `an organisation credential yields an organisation identity`() {
        val seal = CscSigningProvider(
            "http://localhost:${server.address.port}/csc/v1", "http://localhost:${server.address.port}/oauth2/token",
            "test-client", "signer@example.com", "secret", "cred-1", SignerKind.ORGANISATION,
        )

        assertEquals(SignerKind.ORGANISATION, seal.completeSession(seal.startSession("signer").id).kind)
    }

    private fun respond(exchange: HttpExchange, body: String, status: Int = 200) {
        requests.add(exchange.requestURI.path to exchange.requestBody.readBytes().decodeToString())
        authorizations.add(exchange.requestHeaders.getFirst("Authorization"))
        val bytes = body.toByteArray()
        exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }

    private fun certificateBase64() = Base64.getEncoder().encodeToString(certificate.encoded)

    private fun signatureBase64(): String {
        val hash = MessageDigest.getInstance("SHA-256").digest("data".toByteArray())
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(keyPair.private)
            update(hash)
        }
        return Base64.getEncoder().encodeToString(signature.sign())
    }

    private fun selfSignedCertificate(): X509Certificate {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        val subject = X500Name("CN=CMR Sign CSC Test")
        val now = System.currentTimeMillis()
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(now),
            java.util.Date(now - DAY_MILLIS),
            java.util.Date(now + TEN_YEARS_MILLIS),
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
