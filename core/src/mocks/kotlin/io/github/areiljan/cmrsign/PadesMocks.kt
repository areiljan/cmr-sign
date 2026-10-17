package io.github.areiljan.cmrsign

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

fun keyPair(): KeyPair =
    KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()

fun certificate(keyPair: KeyPair): X509Certificate {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
        Security.addProvider(BouncyCastleProvider())
    }

    val subject = X500Name("CN=CMR Sign Test Signer")
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

fun unsignedPdf(): ByteArray =
    requireNotNull(object {}.javaClass.getResourceAsStream("/example-unsigned.pdf")) {
        "missing test resource: example-unsigned.pdf"
    }.use { it.readBytes() }

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
private const val TEN_YEARS_MILLIS = 3650L * DAY_MILLIS
