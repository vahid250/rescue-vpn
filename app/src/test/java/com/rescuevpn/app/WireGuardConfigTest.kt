package com.rescuevpn.app

import com.rescuevpn.app.data.model.WireGuardValidator
import org.junit.Assert.*
import org.junit.Test

class WireGuardConfigTest {

    private val validConfig = """
        [Interface]
        PrivateKey = AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=
        Address = 10.0.0.2/32
        DNS = 1.1.1.1, 9.9.9.9

        [Peer]
        PublicKey = ISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0A=
        Endpoint = 185.143.232.1:53
        AllowedIPs = 0.0.0.0/0
    """.trimIndent()

    @Test
    fun validateAndParse_validConfig_succeeds() {
        val profile = WireGuardValidator.validateAndParse("p_1", "سرور تست", validConfig)
        assertEquals("p_1", profile.id)
        assertEquals("سرور تست", profile.name)
        assertTrue(profile.endpoint.contains("185.143.232.1:53"))
    }

    @Test
    fun validateAndParse_ignoresAllKnownProviderMetadata_succeeds() {
        val configWithMetadata = """
            [Interface]
            PrivateKey = AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=
            Address = 10.0.0.2/32
            DNS = 1.1.1.1
            Bouncing=5#
            NAT-PMP (Port Forwarding)=off#
            VPN Accelerator = on #

            [Peer]
            PublicKey = ISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0A=
            Endpoint = 185.143.232.1:53
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()

        val profile = WireGuardValidator.validateAndParse("p_meta", "metadata test", configWithMetadata)
        assertTrue(profile.endpoint.contains("185.143.232.1:53"))
        assertFalse(profile.rawText.contains("Bouncing"))
        assertFalse(profile.rawText.contains("NAT-PMP"))
        assertFalse(profile.rawText.contains("VPN Accelerator"))
        assertTrue(profile.rawText.contains("[Peer]"))
    }

    @Test
    fun normalizeForWireGuard_removesProviderMetadataFromPreviouslySavedConfigs() {
        val oldSavedConfig = validConfig + "\nBouncing=5#\nNAT-PMP (Port Forwarding)=off#\nVPN Accelerator = on #"
        val normalized = WireGuardValidator.normalizeForWireGuard(oldSavedConfig)
        assertFalse(normalized.contains("Bouncing"))
        assertFalse(normalized.contains("NAT-PMP"))
        assertFalse(normalized.contains("VPN Accelerator"))
        assertTrue(normalized.contains("[Interface]"))
        assertTrue(normalized.contains("[Peer]"))
    }

    @Test(expected = Exception::class)
    fun validateAndParse_invalidConfig_throwsException() {
        WireGuardValidator.validateAndParse("p_err", "نامعتبر", "invalid content without peer")
    }
}
