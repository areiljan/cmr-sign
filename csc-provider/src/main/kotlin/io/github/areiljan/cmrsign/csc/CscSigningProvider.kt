package io.github.areiljan.cmrsign.csc

import io.github.areiljan.cmrsign.Activation
import io.github.areiljan.cmrsign.ProviderSession
import io.github.areiljan.cmrsign.SessionStatus
import io.github.areiljan.cmrsign.SignerIdentity
import io.github.areiljan.cmrsign.SignerKind
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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * [SigningProvider] backed by a Cloud Signature Consortium (CSC) API v1 server.
 *
 * Credentials with `authMode: implicit` are authorized when the session starts. Credentials with
 * `authMode: explicit` and an OTP start [SessionStatus.PENDING]; an online OTP is requested from the
 * provider with `credentials/sendOTP`. The OTP is held in memory until the one hash it authorizes is
 * signed. PIN-protected and `oauth2code` credentials are rejected rather than half supported.
 */
class CscSigningProvider @JvmOverloads constructor(
    private val baseUrl: String,
    private val tokenUrl: String,
    private val clientId: String,
    private val username: String,
    private val password: String,
    private val credentialId: String,
    private val kind: SignerKind = SignerKind.PERSON,
    private val sessionTtl: Duration = Duration.ofMinutes(5),
    private val clock: Clock = Clock.systemUTC(),
) : SigningProvider {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    private val http = HttpClient.newHttpClient()
    private val certificateFactory = CertificateFactory.getInstance("X.509")
    private val sessions = ConcurrentHashMap<String, Session>()

    override fun startSession(signer: String): ProviderSession {
        require(signer.isNotBlank()) { "signer must not be blank" }
        val token = token()
        val info = json.decodeFromString<CredentialsInfoResponse>(
            post("/credentials/info", json.encodeToString(CredentialsInfoRequest(credentialId)), token),
        )
        val activation = activationFor(info)
        if (activation == Activation.OTP && info.OTP?.type == "online") {
            post("/credentials/sendOTP", json.encodeToString(SendOtpRequest(credentialId)), token)
        }
        val session = Session(
            id = UUID.randomUUID().toString(),
            identity = signerIdentity(info),
            activation = activation,
            expiresAt = clock.instant().plus(sessionTtl),
            status = if (activation == Activation.NONE) SessionStatus.AUTHORIZED else SessionStatus.PENDING,
        )
        sessions[session.id] = session
        return session.view()
    }

    override fun session(session: String): ProviderSession = known(session).view()

    override fun authorize(session: String, activationCode: String): ProviderSession {
        require(activationCode.isNotBlank()) { "activation code must not be blank" }
        val current = known(session)
        synchronized(current) {
            check(current.status() == SessionStatus.PENDING) { "session is ${current.status()}, not PENDING" }
            current.otp = activationCode
            current.status = SessionStatus.AUTHORIZED
        }
        return current.view()
    }

    override fun completeSession(session: String): SignerIdentity = authorized(session).identity

    override fun sign(session: String, dataToSign: ByteArray): ByteArray {
        val current = authorized(session)
        val token = token()
        val hash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(dataToSign))
        val otp = synchronized(current) { current.otp.also { current.otp = null } }
        val sad = try {
            json.decodeFromString<AuthorizeResponse>(
                post("/credentials/authorize", json.encodeToString(AuthorizeRequest(credentialId, listOf(hash), OTP = otp)), token),
            ).SAD
        } catch (e: IOException) {
            current.status = SessionStatus.FAILED
            throw e
        }
        val response = json.decodeFromString<SignHashResponse>(
            post("/signatures/signHash", json.encodeToString(SignHashRequest(credentialId, sad, listOf(hash))), token),
        )
        require(response.signatures.isNotEmpty()) { "CSC server returned no signatures" }
        return Base64.getDecoder().decode(response.signatures.first())
    }

    private fun activationFor(info: CredentialsInfoResponse): Activation = when (info.authMode ?: "implicit") {
        "implicit" -> Activation.NONE
        "explicit" -> {
            if (info.PIN?.presence == "true") throw UnsupportedOperationException("PIN-protected CSC credentials are not supported")
            if (info.OTP?.presence == "true") Activation.OTP else Activation.NONE
        }
        else -> throw UnsupportedOperationException("CSC authMode ${info.authMode} is not supported")
    }

    private fun known(session: String) = requireNotNull(sessions[session]) { "unknown session: $session" }

    private fun authorized(session: String) = known(session).also {
        check(it.status() == SessionStatus.AUTHORIZED) { "session is ${it.status()}, not AUTHORIZED" }
    }

    private fun signerIdentity(info: CredentialsInfoResponse): SignerIdentity {
        val certificates = info.cert.certificates.map { encoded ->
            certificateFactory.generateCertificate(Base64.getDecoder().decode(encoded).inputStream()) as X509Certificate
        }
        require(certificates.isNotEmpty()) { "CSC credential has no certificate" }
        return SignerIdentity(
            subjectName = certificates.first().subjectX500Principal.name,
            certificateChain = certificates,
            proofingMethod = "csc",
            kind = kind,
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

    private inner class Session(
        val id: String,
        val identity: SignerIdentity,
        val activation: Activation,
        val expiresAt: Instant,
        @Volatile var status: SessionStatus,
        @Volatile var otp: String? = null,
    ) {
        fun status() = if (status != SessionStatus.FAILED && !clock.instant().isBefore(expiresAt)) SessionStatus.EXPIRED else status

        fun view() = ProviderSession(id, status(), activation, expiresAt)
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
private data class CredentialsInfoResponse(
    val cert: CertificateInfo,
    val authMode: String? = null,
    val PIN: Presence? = null,
    val OTP: Presence? = null,
)

@Serializable
private data class Presence(val presence: String? = null, val type: String? = null)

@Serializable
private data class CertificateInfo(val certificates: List<String>)

@Serializable
private data class SendOtpRequest(val credentialID: String)

@Serializable
private data class AuthorizeRequest(
    val credentialID: String,
    val hash: List<String>,
    val numSignatures: Int = 1,
    val OTP: String? = null,
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
