package com.rescuevpn.app.core.vpn

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Collision-resistant in-memory identity for normalized WireGuard configuration.
 * This digest is only an equality aid; it is not an authentication mechanism.
 * Never log or persist it as a substitute for protecting the configuration itself.
 */
internal object TunnelConfigIdentity {
    fun sha256(normalizedConfig: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalizedConfig.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }
}
