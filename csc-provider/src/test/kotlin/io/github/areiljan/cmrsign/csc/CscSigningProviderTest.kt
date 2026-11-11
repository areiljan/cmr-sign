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
    private val signHashBodies = mutableListOf<String>()
    private val authorizeRequests = mutableListOf<String>()

    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val certificate = selfSignedCertificate()

    private lateinit var provider: CscSigningProvider

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/credentials/info") { exchange ->
                respond(exchange, """{"cert": {"certificates": ["${certificateBase64()}"]}}""")
            }
            createContext("/oauth2/authorize") { exchange ->
                authorizeRequests.add(exchange.requestBody.readBytes().decodeToString())
                respond(exchange, """{"SAD": "sad-123"}""")
            }
            createContext("/signatures/signHash") { exchange ->
                signHashBodies.add(exchange.requestBody.readBytes().decodeToString())
                respond(exchange, """{"signatures": ["${signatureBase64()}"]}""")
            }
            start()
        }
        provider = CscSigningProvider(
            baseUrl = "http://localhost:${server.address.port}",
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
    fun `sign sends the sha-256 hash of the data to signHash`() {
        val signature = provider.sign(provider.startSession("signer"), "data".toByteArray())
        assertTrue(signature.isNotEmpty())
        val expectedHash = MessageDigest.getInstance("SHA-256").digest("data".toByteArray())
        assertTrue(signHashBodies.single().contains(Base64.getEncoder().encodeToString(expectedHash)))
    }

    @Test
    fun `sign authorizes before signing`() {
        provider.sign(provider.startSession("signer"), "data".toByteArray())
        assertEquals(1, authorizeRequests.size)
        assertTrue(authorizeRequests.single().contains("cred-1"))
        assertEquals(1, signHashBodies.size)
    }

    private fun respond(exchange: HttpExchange, body: String) {
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
