package com.rescuevpn.app.data.model

import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

data class WireGuardProfile(
    val id: String,
    val name: String,
    val rawText: String,
    val endpoint: String,
    val allowedIps: String,
    val dns: String,
    val wgConfig: Config
)

object WireGuardValidator {
    /**
     * Removes only the documented provider-specific metadata lines shown in
     * exported client files. All standard WireGuard directives are preserved.
     * This function is also used by the connection service for older saved profiles.
     */
    fun normalizeForWireGuard(rawContent: String): String {
        val providerMetadataKeys = setOf(
            "bouncing",
            "nat-pmp (port forwarding)",
            "vpn accelerator"
        )
        return rawContent
            .removePrefix("\uFEFF")
            .lineSequence()
            .filterNot { line ->
                val key = line.substringBefore('=').trim().lowercase()
                key in providerMetadataKeys
            }
            .joinToString("\n")
            .trim()
    }

    /**
     * Parse and validate a standard WireGuard configuration using the official parser.
     */
    @Throws(Exception::class)
    fun validateAndParse(id: String, name: String, rawContent: String): WireGuardProfile {
        val cleanContent = normalizeForWireGuard(rawContent)
        if (cleanContent.isBlank()) {
            throw IllegalArgumentException("متن کانفیگ نمی‌تواند خالی باشد.")
        }

        val inputStream = ByteArrayInputStream(cleanContent.toByteArray(StandardCharsets.UTF_8))
        val parsedConfig = Config.parse(inputStream)

        val peers = parsedConfig.peers
        if (peers.isEmpty()) {
            throw IllegalArgumentException("فایل کانفیگ فاقد بخش [Peer] معتبر است.")
        }

        val peer = peers[0]
        val endpoint = peer.endpoint.map { it.toString() }.orElse("تعیین‌نشده")
        val allowedIps = peer.allowedIps.joinToString(", ") { it.toString() }
        val dnsServers = parsedConfig.`interface`.dnsServers.joinToString(", ") { it.hostAddress ?: "" }

        return WireGuardProfile(
            id = id,
            name = name.ifBlank { "کانفیگ $id" },
            rawText = cleanContent,
            endpoint = endpoint,
            allowedIps = if (allowedIps.isBlank()) "0.0.0.0/0" else allowedIps,
            dns = if (dnsServers.isBlank()) "1.1.1.1, 9.9.9.9" else dnsServers,
            wgConfig = parsedConfig
        )
    }
}
