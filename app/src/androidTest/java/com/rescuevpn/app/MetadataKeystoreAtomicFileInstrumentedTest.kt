package com.rescuevpn.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rescuevpn.app.data.model.FixedEgressMetadata
import com.rescuevpn.app.data.security.AndroidAtomicRecordFileFactory
import com.rescuevpn.app.data.security.AndroidKeystoreMetadataKeyProvider
import com.rescuevpn.app.data.security.EncryptedMetadataRecordStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MetadataKeystoreAtomicFileInstrumentedTest {

    @Test
    fun keystoreKeyIsCreatedLazilyAndAtomicEncryptedRecordRoundTrips() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val alias = "com.rescuevpn.app.test.metadata.${UUID.randomUUID()}"
        val profileId = UUID.randomUUID().toString()
        val provider = AndroidKeystoreMetadataKeyProvider(alias)
        val store = EncryptedMetadataRecordStore(
            AndroidAtomicRecordFileFactory(context),
            provider
        )

        assertNull("A fresh alias must not exist before first use.", provider.getExistingKey())
        try {
            provider.getOrCreateKey()
            assertNotNull("The key must be retrievable after creation.", provider.getExistingKey())

            val expected = FixedEgressMetadata()
            store.save(profileId, expected)
            assertEquals(expected, store.load(profileId))

            // A separately constructed provider/store must reuse the same persisted Keystore key.
            val secondProvider = AndroidKeystoreMetadataKeyProvider(alias)
            assertNotNull(secondProvider.getExistingKey())
            val secondStore = EncryptedMetadataRecordStore(
                AndroidAtomicRecordFileFactory(context),
                secondProvider
            )
            assertEquals(expected, secondStore.load(profileId))

            store.delete(profileId)
            assertNull(store.load(profileId))
        } finally {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        }
    }
}
