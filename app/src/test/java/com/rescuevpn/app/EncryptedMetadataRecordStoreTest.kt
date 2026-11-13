package com.rescuevpn.app

import com.rescuevpn.app.data.model.EgressMode
import com.rescuevpn.app.data.model.FixedEgressMetadata
import com.rescuevpn.app.data.model.GatewayOwnership
import com.rescuevpn.app.data.security.AtomicRecordFile
import com.rescuevpn.app.data.security.AtomicRecordFileFactory
import com.rescuevpn.app.data.security.EncryptedMetadataRecordStore
import com.rescuevpn.app.data.security.MetadataKeyProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class EncryptedMetadataRecordStoreTest {
    private val key = SecretKeySpec(ByteArray(32) { (it + 3).toByte() }, "AES")
    private val provider = FakeKeyProvider(key)
    private val files = FakeRecordFileFactory()
    private val store = EncryptedMetadataRecordStore(files, provider)

    private val metadata = FixedEgressMetadata(
        mode = EgressMode.CUSTOMER_OWNED_FIXED,
        gatewayOwnership = GatewayOwnership.CUSTOMER,
        expectedEgressIpv4 = "8.8.8.8",
        gatewayLabel = "Gateway شخصی"
    )

    @Test fun savesAndReadsBackAuthenticatedMetadata() {
        store.save("profile-a", metadata)
        assertEquals(metadata, store.load("profile-a"))
    }

    @Test fun absentRecordDoesNotCreateOrRequireKeyOnRead() {
        assertNull(store.load("profile-a"))
        assertEquals(0, provider.createCalls)
    }

    @Test fun missingKeyWithExistingRecordFailsWithoutGeneratingReplacement() {
        store.save("profile-a", metadata)
        provider.removeKey = true
        assertThrows(SecurityException::class.java) { store.load("profile-a") }
        assertThrows(SecurityException::class.java) { store.save("profile-a", FixedEgressMetadata()) }
        assertEquals(1, provider.createCalls)
    }

    @Test fun wrongProfileCannotReadAnotherProfilesRecord() {
        store.save("profile-a", metadata)
        assertThrows(SecurityException::class.java) { store.load("profile-b") }
    }

    @Test fun interruptedWritePreservesPreviousRecord() {
        store.save("profile-a", metadata)
        files.failNextWrite = true
        assertThrows(IllegalStateException::class.java) {
            store.save("profile-a", FixedEgressMetadata())
        }
        assertEquals(metadata, store.load("profile-a"))
    }

    @Test fun deleteRemovesOnlyRequestedMetadataRecord() {
        store.save("profile-a", metadata)
        store.save("profile-b", FixedEgressMetadata())
        store.delete("profile-a")
        assertNull(store.load("profile-a"))
        assertEquals(FixedEgressMetadata(), store.load("profile-b"))
    }

    private class FakeKeyProvider(private val key: SecretKey) : MetadataKeyProvider {
        var removeKey = false
        var createCalls = 0

        override fun getExistingKey(): SecretKey? = if (removeKey) null else key

        override fun getOrCreateKey(): SecretKey {
            createCalls++
            return key
        }
    }

    private class FakeRecordFileFactory : AtomicRecordFileFactory {
        private val records = mutableMapOf<String, ByteArray>()
        var failNextWrite = false

        override fun forProfile(profileId: String): AtomicRecordFile = object : AtomicRecordFile {
            override fun read(): ByteArray? = records[profileId]?.copyOf()

            override fun write(contents: ByteArray) {
                if (failNextWrite) {
                    failNextWrite = false
                    throw IllegalStateException("Simulated interrupted atomic write.")
                }
                records[profileId] = contents.copyOf()
            }

            override fun delete() {
                records.remove(profileId)
            }
        }
    }
}
