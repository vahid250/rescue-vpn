package com.rescuevpn.app

import com.rescuevpn.app.core.vpn.ProfilePinningPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfilePinningPolicyTest {

    @Test
    fun noPinnedProfile_allowsStandardProfileSelection() {
        assertEquals(
            ProfilePinningPolicy.Decision.ALLOW,
            ProfilePinningPolicy.evaluate(null, "standard-1")
        )
    }

    @Test
    fun selectingThePinnedProfile_isAllowed() {
        assertEquals(
            ProfilePinningPolicy.Decision.ALLOW,
            ProfilePinningPolicy.evaluate("fixed-1", "fixed-1")
        )
    }

    @Test
    fun selectingAnotherProfile_isBlockedWhilePinned() {
        assertEquals(
            ProfilePinningPolicy.Decision.BLOCKED_BY_PIN,
            ProfilePinningPolicy.evaluate("fixed-1", "standard-1")
        )
    }

    @Test
    fun explicitUnpin_allowsSwitchingToAnotherProfile() {
        assertEquals(
            ProfilePinningPolicy.Decision.ALLOW,
            ProfilePinningPolicy.evaluate(
                pinnedProfileId = "fixed-1",
                requestedProfileId = "standard-1",
                explicitUnpin = true
            )
        )
    }

    @Test
    fun missingOrBlankRequestedId_isRejectedEvenWhenUnpinIsRequested() {
        assertEquals(
            ProfilePinningPolicy.Decision.INVALID_PROFILE_ID,
            ProfilePinningPolicy.evaluate("fixed-1", null, explicitUnpin = true)
        )
        assertEquals(
            ProfilePinningPolicy.Decision.INVALID_PROFILE_ID,
            ProfilePinningPolicy.evaluate(null, "  ")
        )
    }

    @Test
    fun blankPinIsTreatedAsNoPin() {
        assertEquals(
            ProfilePinningPolicy.Decision.ALLOW,
            ProfilePinningPolicy.evaluate("  ", "standard-1")
        )
    }
}
