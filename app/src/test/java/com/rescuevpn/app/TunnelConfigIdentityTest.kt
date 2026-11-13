package com.rescuevpn.app

import com.rescuevpn.app.core.vpn.TunnelConfigIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TunnelConfigIdentityTest {

    @Test
    fun sha256_isStableForSameNormalizedConfiguration() {
        val config = "[Interface]\nAddress = 10.0.0.2/32\n"
        assertEquals(
            TunnelConfigIdentity.sha256(config),
            TunnelConfigIdentity.sha256(config)
        )
    }

    @Test
    fun sha256_distinguishesDifferentConfigurationsWithSameJavaHashCode() {
        // A known String.hashCode collision: "Aa" and "BB" both hash to 2112.
        assertEquals("Aa".hashCode(), "BB".hashCode())
        assertNotEquals(
            TunnelConfigIdentity.sha256("Aa"),
            TunnelConfigIdentity.sha256("BB")
        )
    }

    @Test
    fun sha256_returnsExpectedDigestLength() {
        assertEquals(64, TunnelConfigIdentity.sha256("test").length)
    }
}
