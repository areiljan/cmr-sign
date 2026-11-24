package io.github.areiljan.cmrsign.csc

import io.github.areiljan.cmrsign.SignerIdentity
import io.github.areiljan.cmrsign.SigningProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * [SigningProvider] backed by a Cloud Signature Consortium (CSC) API v1 server.
 */
class CscSigningProvider(
    private val baseUrl: String,
    private val tokenUrl: String,
    private val clientId: String,
    private val username: String,
    private val password: String,
    private val credentialId: String,
) : SigningProvider {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
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
        return identities.computeIfAbsent(session) { signerIdentity(token()) }
    }

    override fun sign(session: String, dataToSign: ByteArray): ByteArray {
        require(sessions.contains(session)) { "unknown session: $session" }
        val token = token()
        val hash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(dataToSign))
        val sad = json.decodeFromString<AuthorizeResponse>(
            post("/credentials/authorize", json.encodeToString(AuthorizeRequest(credentialId, listOf(hash))), token),
        ).SAD
        val response = json.decodeFromString<SignHashResponse>(
            post("/signatures/signHash", json.encodeToString(SignHashRequest(credentialId, sad, listOf(hash))), token),
        )
        require(response.signatures.isNotEmpty()) { "CSC server returned no signatures" }
        return Base64.getDecoder().decode(response.signatures.first())
    }

    private fun signerIdentity(token: String): SignerIdentity {
        val info = json.decodeFromString<CredentialsInfoResponse>(
            post("/credentials/info", json.encodeToString(CredentialsInfoRequest(credentialId)), token),
        )
        val certificates = info.cert.certificates.map { encoded ->
            certificateFactory.generateCertificate(Base64.getDecoder().decode(encoded).inputStream()) as X509Certificate
        }
        return SignerIdentity(
            subjectName = certificates.first().subjectX500Principal.name,
            certificateChain = certificates,
            proofingMethod = "csc",
        )
    }

    private fun token(): String {
        val form = mapOf("grant_type" to "password", "client_id" to clientId, "username" to username, "password" to password)
            .entries.joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, Charsets.UTF_8)}" }
        val request = HttpRequest.newBuilder(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(BodyPublishers.ofString(form))
            .build()
        return json.decodeFromString<TokenResponse>(send(request)).accessToken
    }

    private fun post(path: String, body: String, token: String): String {
        val request = HttpRequest.newBuilder(URI.create("$baseUrl$path"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .POST(BodyPublishers.ofString(body))
            .build()
        return send(request)
    }

    private fun send(request: HttpRequest): String {
        val response = http.send(request, ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("CSC call to ${request.uri().path} failed with HTTP ${response.statusCode()}: ${response.body()}")
        }
        return response.body()
    }
}

@Serializable
private data class TokenResponse(@SerialName("access_token") val accessToken: String)

@Serializable
private data class CredentialsInfoRequest(
    val credentialID: String,
    val certificates: String = "chain",
)

@Serializable
private data class CredentialsInfoResponse(val cert: CertificateInfo)

@Serializable
private data class CertificateInfo(val certificates: List<String>)

@Serializable
private data class AuthorizeRequest(
    val credentialID: String,
    val hash: List<String>,
    val numSignatures: Int = 1,
)

@Serializable
private data class AuthorizeResponse(val SAD: String)

@Serializable
private data class SignHashRequest(
    val credentialID: String,
    val SAD: String,
    val hash: List<String>,
    val hashAlgo: String = SHA256_OID,
    val signAlgo: String = SHA256_WITH_RSA_OID,
)

@Serializable
private data class SignHashResponse(val signatures: List<String>)

private const val SHA256_OID = "2.16.840.1.101.3.4.2.1"
private const val SHA256_WITH_RSA_OID = "1.2.840.113549.1.1.11"
