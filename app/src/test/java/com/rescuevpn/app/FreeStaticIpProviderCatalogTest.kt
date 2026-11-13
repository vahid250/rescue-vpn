package com.rescuevpn.app

import com.rescuevpn.app.data.model.FreeStaticIpProviderCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeStaticIpProviderCatalogTest {

    @Test
    fun oracleAlwaysFreeIsListedAsConditionalCandidate() {
        val oracle = FreeStaticIpProviderCatalog.providers.firstOrNull {
            it.id == "oracle-always-free"
        }

        assertNotNull(oracle)
        assertTrue(oracle!!.status.contains("مشروط"))
        assertTrue(oracle.officialUrl.startsWith("https://"))
        assertTrue(oracle.ipBehavior.contains("رزروشده"))
    }

    @Test
    fun googleCloudIsNotMisrepresentedAsFreeStaticIpv4() {
        val google = FreeStaticIpProviderCatalog.providers.firstOrNull {
            it.id == "google-cloud"
        }

        assertNotNull(google)
        assertTrue(google!!.status.contains("رایگان نیست"))
        assertFalse(google.status.contains("رایگان مشروط"))
    }
}
