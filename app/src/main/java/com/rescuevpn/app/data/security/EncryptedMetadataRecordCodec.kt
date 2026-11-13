package com.rescuevpn.app.data.security

import com.rescuevpn.app.data.model.FixedEgressMetadata
import com.rescuevpn.app.data.model.FixedEgressMetadataCodec
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Authenticated encryption envelope for fixed-egress metadata.
 *
 * The caller supplies a non-exportable Android Keystore AES key in production.
 * This class does not create, persist, rotate, or recover keys and does not
 * provide durable storage. Tests may inject an ephemeral test key.
 */
internal class EncryptedMetadataRecordCodec {
    fun encrypt(profileId: String, metadata: FixedEgressMetadata, key: SecretKey): String {
        require(validProfileId(profileId)) { "Invalid profile ID." }
        val plaintext = FixedEgressMetadataCodec.encode(profileId, metadata)
            .toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // Android Keystore requires the provider to generate the GCM IV when
        // randomized encryption is enforced. Read that IV back after init.
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = requireNotNull(cipher.iv) { "Encryption provider did not return a GCM nonce." }
        require(iv.size == GCM_IV_BYTES) { "Encryption provider returned an invalid GCM nonce length." }
        cipher.updateAAD(associatedData(profileId))
        val ciphertext = cipher.doFinal(plaintext)

        return listOf(
            HEADER,
            "format_version=$FORMAT_VERSION",
            "iv=" + encodeBytes(iv),
            "ciphertext=" + encodeBytes(ciphertext)
        ).joinToString("\n")
    }

    fun decrypt(profileId: String, record: String, key: SecretKey): FixedEgressMetadata {
        require(validProfileId(profileId)) { "Invalid profile ID." }
        require(record.length <= MAX_ENVELOPE_CHARS) { "Encrypted metadata envelope exceeds the size limit." }
        require('\r' !in record) { "Encrypted metadata envelope contains unsupported line endings." }

        val lines = record.split('\n')
        require(lines.size == 4 && lines[0] == HEADER) { "Invalid encrypted metadata envelope." }
        require(lines[1] == "format_version=$FORMAT_VERSION") {
            "Unsupported encrypted metadata envelope version."
        }
        require(lines[2].startsWith("iv=") && lines[3].startsWith("ciphertext=")) {
            "Malformed encrypted metadata envelope fields."
        }

        val iv = decodeBytes(lines[2].removePrefix("iv="))
        val ciphertext = decodeBytes(lines[3].removePrefix("ciphertext="))
        require(iv.size == GCM_IV_BYTES) { "Invalid encrypted metadata nonce length." }
        require(ciphertext.size in GCM_TAG_BYTES..MAX_CIPHERTEXT_BYTES) {
            "Invalid encrypted metadata ciphertext length."
        }

        val plaintext = try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(associatedData(profileId))
            cipher.doFinal(ciphertext)
        } catch (error: Exception) {
            throw SecurityException("Encrypted metadata authentication failed.", error)
        }

        val decodedText = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(plaintext))
                .toString()
        } catch (error: Exception) {
            throw SecurityException("Authenticated metadata payload is not valid UTF-8.", error)
        }

        return FixedEgressMetadataCodec.decode(profileId, decodedText)
    }

    private fun associatedData(profileId: String): ByteArray =
        "$AAD_DOMAIN|$FORMAT_VERSION|$profileId".toByteArray(StandardCharsets.UTF_8)

    private fun encodeBytes(value: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    private fun decodeBytes(value: String): ByteArray {
        require(value.isNotEmpty()) { "Missing encrypted metadata bytes." }
        val decoded = try {
            Base64.getUrlDecoder().decode(value)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Malformed encrypted metadata Base64.", error)
        }
        require(encodeBytes(decoded) == value) { "Non-canonical encrypted metadata Base64." }
        return decoded
    }

    private fun validProfileId(value: String): Boolean =
        value.isNotBlank() && value.length <= MAX_PROFILE_ID_CHARS &&
            value.none { it.isISOControl() }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val HEADER = "RMA_ENCRYPTED_FIXED_EGRESS_METADATA"
        const val FORMAT_VERSION = "1"
        const val AAD_DOMAIN = "R&MA_FIXED_EGRESS_METADATA"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = 16
        const val MAX_PROFILE_ID_CHARS = 128
        const val MAX_CIPHERTEXT_BYTES = 8192
        const val MAX_ENVELOPE_CHARS = 12000
    }
}
