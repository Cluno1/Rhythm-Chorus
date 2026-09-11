package io.github.cluno1.sonorus.features.catalog.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class CatalogSmartEnrollmentPayload(
    val serverUrl: String,
    val inviteCode: String,
)

enum class CatalogSmartEnrollmentError {
    INPUT_TOO_LONG,
    TOKEN_MISSING,
    MULTIPLE_TOKENS,
    TOKEN_TOO_LONG,
    INVALID_TOKEN,
    UNSUPPORTED_ADDRESS,
}

class CatalogSmartEnrollmentException(
    val reason: CatalogSmartEnrollmentError,
    cause: Throwable? = null,
) : IllegalArgumentException(reason.name, cause)

/**
 * Compact, authenticated obfuscation for sharing a server origin and a one-time invitation.
 *
 * The key is deliberately shared by every app installation. It prevents casual disclosure and
 * detects damaged clipboard text; it is not an authentication secret and cannot resist APK
 * reverse engineering. Server-side invitation expiry and device proof remain the security boundary.
 */
object CatalogSmartEnrollmentText {
    const val TOKEN_PREFIX = "S1A."
    const val MAX_TOKEN_LENGTH = 96
    const val MAX_INPUT_LENGTH = 4 * 1024

    private const val NONCE_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128
    private const val INVITE_LENGTH = 24
    private const val FLAG_HTTPS = 0x04
    private const val HOST_TYPE_MASK = 0x03
    private const val HOST_IPV4 = 0
    private const val HOST_DOMAIN = 1
    private const val HOST_IPV6 = 2
    private const val RESERVED_FLAG_MASK = 0xF8
    private const val MAX_DOMAIN_BYTES = 63
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_BASE64_URL = "ZfB20nxbdcvIoaG9bC-Pu-uO_6VuObR-CD5ljkdbSjc"
    private val aad = "SONORUS-SMART-ENROLLMENT-V1".toByteArray(StandardCharsets.US_ASCII)
    private val tokenCharacters = Regex("[A-Za-z0-9_-]")
    private val secureRandom = SecureRandom()
    private val key by lazy {
        SecretKeySpec(Base64.getUrlDecoder().decode(KEY_BASE64_URL), "AES")
    }

    fun encode(
        normalizedServerUrl: String,
        inviteCode: String,
        nonce: ByteArray = ByteArray(NONCE_LENGTH).also(secureRandom::nextBytes),
    ): String {
        if (nonce.size != NONCE_LENGTH) invalid()
        val plaintext = encodePayload(normalizedServerUrl, inviteCode)
        val encrypted = runCatching {
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, nonce))
                updateAAD(aad)
                doFinal(plaintext)
            }
        }.getOrElse { invalid(it) }
        val token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce + encrypted)
        if (token.length > MAX_TOKEN_LENGTH) {
            throw CatalogSmartEnrollmentException(CatalogSmartEnrollmentError.TOKEN_TOO_LONG)
        }
        return token
    }

    fun decodeFromText(input: String): CatalogSmartEnrollmentPayload {
        val token = extractToken(input)
        val encoded = token.removePrefix(TOKEN_PREFIX)
        val encrypted = runCatching { Base64.getUrlDecoder().decode(encoded) }
            .getOrElse { invalid(it) }
        if (encrypted.size <= NONCE_LENGTH + TAG_LENGTH_BITS / 8) invalid()
        val nonce = encrypted.copyOfRange(0, NONCE_LENGTH)
        val ciphertext = encrypted.copyOfRange(NONCE_LENGTH, encrypted.size)
        val plaintext = runCatching {
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, nonce))
                updateAAD(aad)
                doFinal(ciphertext)
            }
        }.getOrElse { invalid(it) }
        return decodePayload(plaintext)
    }

    private fun extractToken(input: String): String {
        if (input.length > MAX_INPUT_LENGTH) {
            throw CatalogSmartEnrollmentException(CatalogSmartEnrollmentError.INPUT_TOO_LONG)
        }
        val first = input.indexOf(TOKEN_PREFIX)
        if (first < 0) throw CatalogSmartEnrollmentException(CatalogSmartEnrollmentError.TOKEN_MISSING)
        if (input.indexOf(TOKEN_PREFIX, first + TOKEN_PREFIX.length) >= 0) {
            throw CatalogSmartEnrollmentException(CatalogSmartEnrollmentError.MULTIPLE_TOKENS)
        }
        var end = first + TOKEN_PREFIX.length
        while (end < input.length && tokenCharacters.matches(input[end].toString())) end++
        if (end == first + TOKEN_PREFIX.length) invalid()
        val token = input.substring(first, end)
        if (token.length > MAX_TOKEN_LENGTH) {
            throw CatalogSmartEnrollmentException(CatalogSmartEnrollmentError.TOKEN_TOO_LONG)
        }
        return token
    }

    private fun encodePayload(normalizedServerUrl: String, inviteCode: String): ByteArray {
        val uri = runCatching { URI(normalizedServerUrl) }.getOrElse { unsupportedAddress(it) }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") unsupportedAddress()
        if (uri.userInfo != null || uri.query != null || uri.fragment != null) unsupportedAddress()
        if (uri.path != null && uri.path != "" && uri.path != "/") unsupportedAddress()
        val host = uri.host?.removePrefix("[")?.removeSuffix("]")?.lowercase()
            ?.takeIf(String::isNotBlank) ?: unsupportedAddress()
        val port = when {
            uri.port in 1..65535 -> uri.port
            uri.port != -1 -> unsupportedAddress()
            scheme == "https" -> 443
            else -> 80
        }
        val invite = runCatching { Base64.getUrlDecoder().decode(inviteCode.trim()) }
            .getOrElse { invalid(it) }
        if (invite.size != INVITE_LENGTH ||
            Base64.getUrlEncoder().withoutPadding().encodeToString(invite) != inviteCode.trim()
        ) {
            invalid()
        }

        val ipv4 = parseIpv4(host)
        val ipv6 = if (ipv4 == null && ':' in host) parseIpv6(host) else null
        val domain = if (ipv4 == null && ipv6 == null) {
            host.toByteArray(StandardCharsets.US_ASCII).also {
                if (it.isEmpty() || it.size > MAX_DOMAIN_BYTES || host.any { char -> char.code > 0x7f }) {
                    unsupportedAddress()
                }
            }
        } else {
            null
        }
        val hostType = when {
            ipv4 != null -> HOST_IPV4
            ipv6 != null -> HOST_IPV6
            else -> HOST_DOMAIN
        }
        val flags = hostType or if (scheme == "https") FLAG_HTTPS else 0

        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeByte(flags)
                when (hostType) {
                    HOST_IPV4 -> output.write(ipv4)
                    HOST_IPV6 -> {
                        val compressed = compressIpv6(ipv6!!)
                        output.writeByte(compressed.zeroStart)
                        output.writeByte(compressed.zeroLength)
                        output.write(compressed.remaining)
                    }
                    HOST_DOMAIN -> {
                        output.writeByte(domain!!.size)
                        output.write(domain)
                    }
                }
                output.writeShort(port)
                output.write(invite)
            }
            bytes.toByteArray()
        }
    }

    private fun decodePayload(bytes: ByteArray): CatalogSmartEnrollmentPayload = runCatching {
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val flags = input.readUnsignedByte()
            if (flags and RESERVED_FLAG_MASK != 0) invalid()
            val host = when (flags and HOST_TYPE_MASK) {
                HOST_IPV4 -> List(4) { input.readUnsignedByte() }.joinToString(".")
                HOST_DOMAIN -> {
                    val length = input.readUnsignedByte()
                    if (length !in 1..MAX_DOMAIN_BYTES) invalid()
                    ByteArray(length).also(input::readFully).toString(StandardCharsets.US_ASCII)
                }
                HOST_IPV6 -> {
                    val zeroStart = input.readUnsignedByte()
                    val zeroLength = input.readUnsignedByte()
                    if (zeroStart !in 0..16 || zeroLength !in 0..16 || zeroStart + zeroLength > 16) invalid()
                    val remaining = ByteArray(16 - zeroLength).also(input::readFully)
                    val address = ByteArray(16)
                    remaining.copyInto(address, endIndex = zeroStart)
                    remaining.copyInto(
                        destination = address,
                        destinationOffset = zeroStart + zeroLength,
                        startIndex = zeroStart,
                    )
                    (InetAddress.getByAddress(address) as? Inet6Address)?.hostAddress ?: invalid()
                }
                else -> invalid()
            }
            val port = input.readUnsignedShort()
            if (port == 0) invalid()
            val invite = ByteArray(INVITE_LENGTH).also(input::readFully)
            if (input.available() != 0) invalid()
            val scheme = if (flags and FLAG_HTTPS != 0) "https" else "http"
            val displayHost = if (':' in host) "[$host]" else host
            val defaultPort = if (scheme == "https") 443 else 80
            val serverUrl = if (port == defaultPort) "$scheme://$displayHost" else "$scheme://$displayHost:$port"
            CatalogSmartEnrollmentPayload(
                serverUrl = serverUrl,
                inviteCode = Base64.getUrlEncoder().withoutPadding().encodeToString(invite),
            )
        }
    }.getOrElse { error ->
        if (error is CatalogSmartEnrollmentException) throw error
        invalid(error)
    }

    private fun parseIpv4(host: String): ByteArray? {
        val parts = host.split('.')
        if (parts.size != 4) return null
        val values = parts.map { part ->
            if (part.isEmpty() || (part.length > 1 && part.startsWith('0'))) return null
            part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
        }
        return ByteArray(4) { values[it].toByte() }
    }

    private fun parseIpv6(host: String): ByteArray? = runCatching {
        (InetAddress.getByName(host) as? Inet6Address)?.address
    }.getOrNull()

    private data class CompressedIpv6(
        val zeroStart: Int,
        val zeroLength: Int,
        val remaining: ByteArray,
    )

    private fun compressIpv6(address: ByteArray): CompressedIpv6 {
        var bestStart = 0
        var bestLength = 0
        var index = 0
        while (index < address.size) {
            if (address[index] != 0.toByte()) {
                index++
                continue
            }
            val start = index
            while (index < address.size && address[index] == 0.toByte()) index++
            if (index - start > bestLength) {
                bestStart = start
                bestLength = index - start
            }
        }
        val remaining = address.copyOfRange(0, bestStart) +
            address.copyOfRange(bestStart + bestLength, address.size)
        return CompressedIpv6(bestStart, bestLength, remaining)
    }

    private fun invalid(cause: Throwable? = null): Nothing =
        throw CatalogSmartEnrollmentException(CatalogSmartEnrollmentError.INVALID_TOKEN, cause)

    private fun unsupportedAddress(cause: Throwable? = null): Nothing =
        throw CatalogSmartEnrollmentException(CatalogSmartEnrollmentError.UNSUPPORTED_ADDRESS, cause)
}
