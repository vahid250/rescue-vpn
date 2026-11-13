package com.rescuevpn.app.data.model

/**
 * Versioned, non-secret metadata for an optional customer-owned fixed-egress
 * WireGuard profile. This model deliberately contains no private keys,
 * credentials, provider tokens, or provisioning data.
 */
data class FixedEgressMetadata(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val mode: EgressMode = EgressMode.STANDARD,
    val gatewayOwnership: GatewayOwnership = GatewayOwnership.NONE,
    val expectedEgressIpv4: String? = null,
    val gatewayLabel: String? = null
) {
    fun validationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            errors += "Unsupported fixed-egress metadata schema version."
        }

        if (gatewayLabel != null) {
            if (gatewayLabel.isBlank() || gatewayLabel != gatewayLabel.trim()) {
                errors += "Gateway label must be non-blank and trimmed."
            }
            if (gatewayLabel.length > MAX_GATEWAY_LABEL_LENGTH) {
                errors += "Gateway label exceeds the maximum length."
            }
            if (gatewayLabel.any { it.isISOControl() }) {
                errors += "Gateway label contains unsupported control characters."
            }
        }

        when (mode) {
            EgressMode.STANDARD -> {
                if (gatewayOwnership != GatewayOwnership.NONE) {
                    errors += "Standard profiles cannot declare a customer-owned gateway."
                }
                if (expectedEgressIpv4 != null) {
                    errors += "Standard profiles cannot pin an expected egress IPv4 address."
                }
            }

            EgressMode.CUSTOMER_OWNED_FIXED -> {
                if (gatewayOwnership != GatewayOwnership.CUSTOMER) {
                    errors += "Fixed egress requires a customer-owned gateway."
                }
                if (expectedEgressIpv4 == null ||
                    !isPublicIpv4Candidate(expectedEgressIpv4)
                ) {
                    errors += "Fixed egress requires a valid public IPv4 address."
                }
            }
        }

        return errors
    }

    fun isValid(): Boolean = validationErrors().isEmpty()

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val MAX_GATEWAY_LABEL_LENGTH = 64

        /**
         * Validates a canonical dotted-decimal IPv4 literal and rejects
         * private, shared, loopback, link-local, documentation, benchmark,
         * multicast and reserved ranges. This is a local format/range check,
         * not proof that an address is assigned, reachable, or owned by the user.
         */
        fun isPublicIpv4Candidate(value: String): Boolean {
            val octets = value.split('.')
            if (octets.size != 4) return false

            val numbers = octets.map { part ->
                if (part.isEmpty() || part.any { !it.isDigit() }) return false
                if (part.length > 1 && part.startsWith('0')) return false
                part.toIntOrNull()?.takeIf { it in 0..255 } ?: return false
            }

            val address = numbers.fold(0L) { acc, octet ->
                (acc shl 8) or octet.toLong()
            }

            val blockedRanges = listOf(
                0x00000000L..0x00FFFFFFL, // 0.0.0.0/8
                0x0A000000L..0x0AFFFFFFL, // 10.0.0.0/8
                0x64400000L..0x647FFFFFL, // 100.64.0.0/10 (shared address space)
                0x7F000000L..0x7FFFFFFFL, // 127.0.0.0/8
                0xA9FE0000L..0xA9FEFFFFL, // 169.254.0.0/16
                0xAC100000L..0xAC1FFFFFL, // 172.16.0.0/12
                0xC0000000L..0xC00000FFL, // 192.0.0.0/24
                0xC0000200L..0xC00002FFL, // 192.0.2.0/24
                0xC0586300L..0xC05863FFL, // 192.88.99.0/24
                0xC0A80000L..0xC0A8FFFFL, // 192.168.0.0/16
                0xC6120000L..0xC613FFFFL, // 198.18.0.0/15
                0xC6336400L..0xC63364FFL, // 198.51.100.0/24
                0xCB007100L..0xCB0071FFL, // 203.0.113.0/24
                0xE0000000L..0xEFFFFFFFL, // 224.0.0.0/4
                0xF0000000L..0xFFFFFFFFL  // 240.0.0.0/4
            )
            return blockedRanges.none { address in it }
        }
    }
}

enum class EgressMode {
    STANDARD,
    CUSTOMER_OWNED_FIXED
}

enum class GatewayOwnership {
    NONE,
    CUSTOMER
}
