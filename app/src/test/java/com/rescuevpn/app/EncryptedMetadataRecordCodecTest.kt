package com.rescuevpn.app

import com.rescuevpn.app.data.model.EgressMode
import com.rescuevpn.app.data.model.FixedEgressMetadata
import com.rescuevpn.app.data.model.GatewayOwnership
import com.rescuevpn.app.data.security.EncryptedMetadataRecordCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class EncryptedMetadataRecordCodecTest {
    private val codec = EncryptedMetadataRecordCodec()
    private val key = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")

    private val fixedMetadata = FixedEgressMetadata(
        mode = EgressMode.CUSTOMER_OWNED_FIXED,
        gatewayOwnership = GatewayOwnership.CUSTOMER,
        expectedEgressIpv4 = "8.8.8.8",
        gatewayLabel = "Personal gateway"
    )

    @Test fun encryptedRecordRoundTripsMetadata() {
        val id = "123e4567-e89b-42d3-a456-426614174000"
        val encrypted = codec.encrypt(id, fixedMetadata, key)
        assertEquals(fixedMetadata, codec.decrypt(id, encrypted, key))
    }

    @Test fun encryptionUsesFreshNonce() {
        val first = codec.encrypt("profile-a", fixedMetadata, key)
        val second = codec.encrypt("profile-a", fixedMetadata, key)
        assertNotEquals(first, second)
    }

    @Test fun wrongProfileIdFailsAuthentication() {
        val encrypted = codec.encrypt("profile-a", fixedMetadata, key)
        assertThrows(SecurityException::class.java) {
            codec.decrypt("profile-b", encrypted, key)
        }
    }

    @Test fun wrongKeyFailsAuthentication() {
        val encrypted = codec.encrypt("profile-a", fixedMetadata, key)
        val otherKey = SecretKeySpec(ByteArray(32) { (it + 9).toByte() }, "AES")
        assertThrows(SecurityException::class.java) {
            codec.decrypt("profile-a", encrypted, otherKey)
        }
    }

    @Test fun ciphertextTamperingFailsAuthentication() {
        val encrypted = codec.encrypt("profile-a", fixedMetadata, key)
        val lines = encrypted.split('\n').toMutableList()
        val field = lines[3]
        val prefixLength = "ciphertext=".length
        val replacementCharacter = if (field[prefixLength] == 'A') "B" else "A"
        lines[3] = field.substring(0, prefixLength) +
            replacementCharacter + field.substring(prefixLength + 1)
        assertThrows(SecurityException::class.java) {
            codec.decrypt("profile-a", lines.joinToString("\n"), key)
        }
    }

    @Test fun malformedAndTruncatedEnvelopesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.decrypt("profile-a", "broken", key)
        }
        val encrypted = codec.encrypt("profile-a", fixedMetadata, key)
        val truncated = encrypted.substringBeforeLast('\n')
        assertThrows(IllegalArgumentException::class.java) {
            codec.decrypt("profile-a", truncated, key)
        }
    }

    @Test fun unsupportedVersionAndInvalidNonceAreRejected() {
        val encrypted = codec.encrypt("profile-a", fixedMetadata, key)
        val wrongVersion = encrypted.replace("format_version=1", "format_version=9")
        assertThrows(IllegalArgumentException::class.java) {
            codec.decrypt("profile-a", wrongVersion, key)
        }
        val invalidIv = encrypted.replace(Regex("iv=[^\\n]+"), "iv=AA")
        assertThrows(IllegalArgumentException::class.java) {
            codec.decrypt("profile-a", invalidIv, key)
        }
    }
}
