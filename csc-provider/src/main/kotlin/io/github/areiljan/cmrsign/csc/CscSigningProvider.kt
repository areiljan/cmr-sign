package io.github.areiljan.cmrsign.csc

import io.github.areiljan.cmrsign.SignerIdentity
import io.github.areiljan.cmrsign.SigningProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.io.IOException
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * [SigningProvider] backed by a Cloud Signature Consortium (CSC) API v2 server.
 */
class CscSigningProvider(
    private val baseUrl: String,
    private val clientId: String,
    private val username: String,
    private val password: String,
    private val credentialId: String,
) : SigningProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient.newHttpClient()
    private val certificateFactory = CertificateFactory.getInstance("X.509")
    private val sessions = ConcurrentHashMap.newKeySet<String>()
    private val identities = ConcurrentHashMap<String, SignerIdentity>()

    override fun startSession(signer: String): String {
        require(signer.isNotBlank()) { "signer must not be blank" }
        return UUID.randomUUID().toString().also { sessions.add(it) }
    }

    override fun completeSession(session: String): SignerIdentity {
        require(sessions.contains(session)) { "unknown session: $session" }
        return identities.computeIfAbsent(session) { signerIdentity() }
    }

    override fun sign(session: String, dataToSign: ByteArray): ByteArray {
        require(sessions.contains(session)) { "unknown session: $session" }
        val sad = authorize()
        val hash = MessageDigest.getInstance("SHA-256").digest(dataToSign)
        val request = SignHashRequest(
            credentialID = credentialId,
            SAD = sad,
            hashes = listOf(Base64.getEncoder().encodeToString(hash)),
        )
        val response = json.decodeFromString<SignHashResponse>(post("/signatures/signHash", json.encodeToString(request)))
        require(response.signatures.isNotEmpty()) { "CSC server returned no signatures" }
        return Base64.getDecoder().decode(response.signatures.first())
    }

    private fun signerIdentity(): SignerIdentity {
        val info = json.decodeFromString<CredentialsInfoResponse>(
            post("/credentials/info", json.encodeToString(CredentialsInfoRequest(credentialID = credentialId))),
        )
        val certificates = info.cert.certificates.map { encoded ->
            certificateFactory.generateCertificate(Base64.getDecoder().decode(encoded).inputStream()) as X509Certificate
        }
        val leaf = certificates.first()
        return SignerIdentity(
            subjectName = leaf.subjectX500Principal.name,
            certificateChain = certificates,
            proofingMethod = "csc",
        )
    }

    private fun authorize(): String {
        val request = AuthorizeRequest(
            clientID = clientId,
            username = username,
            password = password,
            credentialID = credentialId,
        )
        val response = json.decodeFromString<AuthorizeResponse>(post("/oauth2/authorize", json.encodeToString(request)))
        return response.SAD
    }

    private fun post(path: String, body: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("CSC call to $path failed with HTTP ${response.statusCode()}: ${response.body()}")
        }
        return response.body()
    }
}

@Serializable
private data class CredentialsInfoRequest(@SerialName("credentialID") val credentialID: String)

@Serializable
private data class CredentialsInfoResponse(@SerialName("cert") val cert: CertificateInfo)

@Serializable
private data class CertificateInfo(@SerialName("certificates") val certificates: List<String>)

@Serializable
private data class AuthorizeRequest(
    @SerialName("clientID") val clientID: String,
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("credentialID") val credentialID: String,
    @SerialName("numSignatures") val numSignatures: Int = 1,
)

@Serializable
private data class AuthorizeResponse(@SerialName("SAD") val SAD: String)

@Serializable
private data class SignHashRequest(
    @SerialName("credentialID") val credentialID: String,
    @SerialName("SAD") val SAD: String,
    @SerialName("hashes") val hashes: List<String>,
    @SerialName("hashAlgorithm") val hashAlgorithm: String = SHA256_OID,
    @SerialName("signAlgo") val signAlgo: String = SHA256_WITH_RSA_OID,
)

@Serializable
private data class SignHashResponse(@SerialName("signatures") val signatures: List<String>)

private const val SHA256_OID = "2.16.840.1.101.3.4.2.1"
private const val SHA256_WITH_RSA_OID = "1.2.840.113549.1.1.11"
