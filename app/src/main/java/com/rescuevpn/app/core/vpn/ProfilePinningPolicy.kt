package com.rescuevpn.app.core.vpn

/**
 * Pure policy gate for a user-pinned fixed-egress profile.
 *
 * This class deliberately has no persistence or Android dependencies. The caller
 * must obtain the pin from validated, encrypted profile metadata and must only
 * pass explicitUnpin=true after a deliberate user action has been confirmed.
 */
internal object ProfilePinningPolicy {

    enum class Decision {
        ALLOW,
        BLOCKED_BY_PIN,
        INVALID_PROFILE_ID
    }

    fun evaluate(
        pinnedProfileId: String?,
        requestedProfileId: String?,
        explicitUnpin: Boolean = false
    ): Decision {
        if (requestedProfileId.isNullOrBlank()) {
            return Decision.INVALID_PROFILE_ID
        }

        if (explicitUnpin) {
            return Decision.ALLOW
        }

        if (pinnedProfileId.isNullOrBlank()) {
            return Decision.ALLOW
        }

        return if (requestedProfileId == pinnedProfileId) {
            Decision.ALLOW
        } else {
            Decision.BLOCKED_BY_PIN
        }
    }
}
