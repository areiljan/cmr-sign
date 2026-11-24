package io.github.areiljan.cmrsign.csc

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Security
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CscSigningProviderTest {
    private lateinit var server: HttpServer
    private val requests = mutableListOf<Pair<String, String>>()
    private val authorizations = mutableListOf<String?>()

    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val certificate = selfSignedCertificate()

    private lateinit var provider: CscSigningProvider

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/oauth2/token") { respond(it, """{"access_token": "token-1"}""") }
            createContext("/csc/v1/credentials/info") { respond(it, """{"cert": {"certificates": ["${certificateBase64()}"]}}""") }
            createContext("/csc/v1/credentials/authorize") { respond(it, """{"SAD": "sad-123"}""") }
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
        )
    }

    @AfterEach
    fun stop() = server.stop(0)

    @Test
    fun `completeSession returns the signer identity from the credential certificate`() {
        val identity = provider.completeSession(provider.startSession("signer"))
        assertEquals(certificate.subjectX500Principal.name, identity.subjectName)
        assertEquals(listOf(certificate), identity.certificateChain)
    }

    @Test
    fun `sign authorizes the hash and then signs it`() {
        val hash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest("data".toByteArray()))

        val signature = provider.sign(provider.startSession("signer"), "data".toByteArray())

        assertTrue(signature.isNotEmpty())
        assertEquals(listOf("/oauth2/token", "/csc/v1/credentials/authorize", "/csc/v1/signatures/signHash"), requests.map { it.first })
        assertTrue(requests[1].second.contains(hash))
        assertTrue(requests[2].second.contains("\"SAD\":\"sad-123\""))
        assertTrue(requests[2].second.contains(hash))
        assertTrue(requests[2].second.contains("\"signAlgo\":\"1.2.840.113549.1.1.11\""))
    }

    @Test
    fun `calls the csc api with the oauth2 access token`() {
        provider.sign(provider.startSession("signer"), "data".toByteArray())

        assertTrue(requests[0].second.contains("grant_type=password"))
        assertEquals(listOf(null, "Bearer token-1", "Bearer token-1"), authorizations)
    }

    private fun respond(exchange: HttpExchange, body: String) {
        requests.add(exchange.requestURI.path to exchange.requestBody.readBytes().decodeToString())
        authorizations.add(exchange.requestHeaders.getFirst("Authorization"))
        exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
        exchange.responseBody.use { it.write(body.toByteArray()) }
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
