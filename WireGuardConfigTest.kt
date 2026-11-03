package com.rescuevpn.app

import com.rescuevpn.app.data.model.WireGuardValidator
import org.junit.Assert.*
import org.junit.Test

class WireGuardConfigTest {

    @Test
    fun validateAndParse_validConfig_succeeds() {
        val validConf = """
            [Interface]
            PrivateKey = aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa=
            Address = 10.0.0.2/32
            DNS = 1.1.1.1, 9.9.9.9

            [Peer]
            PublicKey = bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb=
            Endpoint = 185.143.232.1:53
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()

        val profile = WireGuardValidator.validateAndParse("p_1", "سرور تست", validConf)
        assertEquals("p_1", profile.id)
        assertEquals("سرور تست", profile.name)
        assertTrue(profile.endpoint.contains("185.143.232.1:53"))
    }

    @Test(expected = Exception::class)
    fun validateAndParse_invalidConfig_throwsException() {
        val invalidConf = "invalid content without peer"
        WireGuardValidator.validateAndParse("p_err", "نامعتبر", invalidConf)
    }
}
