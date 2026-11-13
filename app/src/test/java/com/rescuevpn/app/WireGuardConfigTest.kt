package com.rescuevpn.app

import com.rescuevpn.app.data.model.WireGuardValidator
import com.rescuevpn.app.core.vpn.TunnelCommand
import com.rescuevpn.app.core.vpn.TunnelCommandPolicy
import com.rescuevpn.app.core.vpn.HandshakeFreshness
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
    @Test
    fun latestTunnelCommand_rapidProfileSwitches_keepsOnlyNewestStart() {
        val result = TunnelCommandPolicy.latest(
            listOf(
                TunnelCommand.Start("A", "config-A"),
                TunnelCommand.Start("B", "config-B"),
                TunnelCommand.Start("C", "config-C")
            )
        )
        assertEquals(TunnelCommand.Start("C", "config-C"), result)
    }

    @Test
    fun latestTunnelCommand_finalStop_cancelsQueuedStarts() {
        val result = TunnelCommandPolicy.latest(
            listOf(
                TunnelCommand.Start("A", "config-A"),
                TunnelCommand.Start("B", "config-B"),
                TunnelCommand.Stop(42)
            )
        )
        assertEquals(TunnelCommand.Stop(42), result)
    }

    @Test
    fun latestTunnelCommand_startAfterStop_representsNewestUserIntent() {
        val result = TunnelCommandPolicy.latest(
            listOf(
                TunnelCommand.Stop(42),
                TunnelCommand.Start("C", "config-C")
            )
        )
        assertEquals(TunnelCommand.Start("C", "config-C"), result)
    }

    @Test
    fun handshakeFreshness_recentHandshake_isRecent() {
        val now = 1_000_000L
        assertTrue(HandshakeFreshness.isRecent(now - 120_000L, now))
        assertEquals(120L, HandshakeFreshness.ageSeconds(now - 120_000L, now))
    }

    @Test
    fun handshakeFreshness_oldHandshake_isNotRecent() {
        val now = 1_000_000L
        assertFalse(HandshakeFreshness.isRecent(now - 181_000L, now))
    }

    @Test
    fun handshakeFreshness_missingOrFutureHandshake_isNotRecent() {
        val now = 1_000_000L
        assertFalse(HandshakeFreshness.isRecent(0L, now))
        assertFalse(HandshakeFreshness.isRecent(now + 1L, now))
        assertNull(HandshakeFreshness.ageSeconds(now + 1L, now))
    }

}
