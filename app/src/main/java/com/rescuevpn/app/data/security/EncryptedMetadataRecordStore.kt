package com.rescuevpn.app.data.security

import com.rescuevpn.app.data.model.FixedEgressMetadata
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

/**
 * Stores only authenticated encrypted metadata in app-private AtomicFile records.
 *
 * This store is deliberately separate from SecureConfigStore: it does not read,
 * rewrite, migrate, or delete existing WireGuard configuration records.
 */
private object MetadataRecordStoreLock

internal class EncryptedMetadataRecordStore(
    private val files: AtomicRecordFileFactory,
    private val keyProvider: MetadataKeyProvider,
    private val codec: EncryptedMetadataRecordCodec = EncryptedMetadataRecordCodec()
) {
    fun save(profileId: String, metadata: FixedEgressMetadata) = synchronized(MetadataRecordStoreLock) {
        val file = files.forProfile(profileId)
        val existing = file.read()
        val key = if (existing == null) {
            keyProvider.getOrCreateKey()
        } else {
            keyProvider.getExistingKey()
                ?: throw SecurityException("Metadata ciphertext exists but its Keystore key is missing; refusing to replace the key.")
        }
        val envelope = codec.encrypt(profileId, metadata, key)
        file.write(envelope.toByteArray(StandardCharsets.UTF_8))
        val written = file.read()
            ?: throw IOException("Encrypted metadata record was not present after atomic write.")
        val verified = decodeEnvelope(written)
        val readBack = codec.decrypt(profileId, verified, key)
        if (readBack != metadata) {
            throw SecurityException("Encrypted metadata read-back verification failed.")
        }
    }

    fun load(profileId: String): FixedEgressMetadata? = synchronized(MetadataRecordStoreLock) {
        val bytes = files.forProfile(profileId).read() ?: return null
        if (bytes.size > MAX_ENVELOPE_BYTES) {
            throw SecurityException("Encrypted metadata record exceeds the size limit.")
        }
        val key: SecretKey = keyProvider.getExistingKey()
            ?: throw SecurityException("Encrypted metadata exists but its Keystore key is missing; refusing to generate a replacement.")
        return codec.decrypt(profileId, decodeEnvelope(bytes), key)
    }

    fun delete(profileId: String) = synchronized(MetadataRecordStoreLock) {
        val file = files.forProfile(profileId)
        if (file.read() != null && keyProvider.getExistingKey() == null) {
            throw SecurityException("Encrypted metadata exists but its Keystore key is missing; refusing an unauthenticated delete.")
        }
        file.delete()
    }

    private fun decodeEnvelope(bytes: ByteArray): String {
        if (bytes.size > MAX_ENVELOPE_BYTES) {
            throw SecurityException("Encrypted metadata record exceeds the size limit.")
        }
        return try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes))
                .toString()
        } catch (error: Exception) {
            throw SecurityException("Encrypted metadata record is not valid UTF-8.", error)
        }
    }

    private companion object {
        const val MAX_ENVELOPE_BYTES = 12000
    }
}
