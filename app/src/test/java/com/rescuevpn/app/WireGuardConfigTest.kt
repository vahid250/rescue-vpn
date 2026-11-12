package com.rescuevpn.app

import com.rescuevpn.app.data.model.WireGuardValidator
import org.junit.Assert.*
import org.junit.Test

class WireGuardConfigTest {

    @Test
    fun validateAndParse_validConfig_succeeds() {
        val validConf = """
            [Interface]
            PrivateKey = AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=
            Address = 10.0.0.2/32
            DNS = 1.1.1.1, 9.9.9.9

            [Peer]
            PublicKey = ISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0A=
            Endpoint = 185.143.232.1:53
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()

        val profile = WireGuardValidator.validateAndParse("p_1", "سرور تست", validConf)
        assertEquals("p_1", profile.id)
        assertEquals("سرور تست", profile.name)
        assertTrue(profile.endpoint.contains("185.143.232.1:53"))
    }

    @Test
    fun validateAndParse_ignoresKnownProviderMetadata_succeeds() {
        val configWithMetadata = """
            [Interface]
            PrivateKey = AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=
            Address = 10.0.0.2/32
            DNS = 1.1.1.1
            VPN Accelerator = on#

            [Peer]
            PublicKey = ISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0A=
            Endpoint = 185.143.232.1:53
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()

        val profile = WireGuardValidator.validateAndParse("p_meta", "metadata test", configWithMetadata)
        assertTrue(profile.endpoint.contains("185.143.232.1:53"))
    }

    @Test(expected = Exception::class)
    fun validateAndParse_invalidConfig_throwsException() {
        val invalidConf = "invalid content without peer"
        WireGuardValidator.validateAndParse("p_err", "نامعتبر", invalidConf)
    }
}
