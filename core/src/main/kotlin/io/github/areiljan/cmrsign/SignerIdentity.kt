package io.github.areiljan.cmrsign

import java.security.cert.X509Certificate
import javax.naming.ldap.LdapName

class SignerIdentity @JvmOverloads constructor(
    val subjectName: String,
    val certificateChain: List<X509Certificate> = emptyList(),
    val proofingMethod: String? = null,
    val kind: SignerKind = SignerKind.PERSON,
) {
    val commonName: String?
        get() = runCatching { LdapName(subjectName).rdns.lastOrNull { it.type.equals("CN", ignoreCase = true) }?.value?.toString() }
            .getOrNull()

    /** True when the certificate's common name is the signer the platform expects. */
    fun isBoundTo(expectedSigner: String): Boolean =
        commonName?.let { normalize(it) == normalize(expectedSigner) } ?: false

    private fun normalize(name: String) = name.trim().replace(Regex("\\s+"), " ").lowercase()
}

enum class SignerKind { PERSON, ORGANISATION }
