package io.github.areiljan.cmrsign.cades

import java.nio.ByteBuffer
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun totp(secret: ByteArray, at: Instant, digits: Int = 6, stepSeconds: Long = 30): String {
    val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(secret, "HmacSHA1")) }
    val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(at.epochSecond / stepSeconds).array())
    val offset = hash.last().toInt() and 0x0f
    val code = ByteBuffer.wrap(hash, offset, 4).int and 0x7fffffff
    return (code % Math.pow(10.0, digits.toDouble()).toInt()).toString().padStart(digits, '0')
}
