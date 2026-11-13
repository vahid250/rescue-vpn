package com.rescuevpn.app.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Lazily obtains the dedicated AES key used only for fixed-egress metadata.
 *
 * Existing aliases are never replaced automatically: if an existing key is
 * unavailable or invalidated, callers receive a security failure rather than
 * creating a new key that would make existing ciphertext appear recoverable.
 */
internal class AndroidKeystoreMetadataKeyProvider(
    private val alias: String = KEY_ALIAS
) : MetadataKeyProvider {

    override fun getOrCreateKey(): SecretKey {
        require(alias.isNotBlank()) { "Keystore alias must not be blank." }

        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            if (keyStore.containsAlias(alias)) {
                val existing = keyStore.getKey(alias, null)
                return existing as? SecretKey
                    ?: throw SecurityException("The existing metadata key is unavailable or has an unexpected type.")
            }

            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
            )
            return generator.generateKey()
        } catch (error: KeyPermanentlyInvalidatedException) {
            throw SecurityException("The fixed-egress metadata key was invalidated; encrypted records cannot be opened.", error)
        } catch (error: UnrecoverableKeyException) {
            throw SecurityException("The existing fixed-egress metadata key cannot be recovered; no replacement was created.", error)
        } catch (error: SecurityException) {
            throw error
        } catch (error: Exception) {
            throw SecurityException("Android Keystore could not provide the fixed-egress metadata key.", error)
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "com.rescuevpn.app.fixed_egress_metadata.v1"
        const val KEY_SIZE_BITS = 256
    }
}
