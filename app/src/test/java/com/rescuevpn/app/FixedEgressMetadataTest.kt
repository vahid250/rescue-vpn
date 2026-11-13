package com.rescuevpn.app

import com.rescuevpn.app.data.model.EgressMode
import com.rescuevpn.app.data.model.FixedEgressMetadata
import com.rescuevpn.app.data.model.GatewayOwnership
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedEgressMetadataTest {

    @Test
    fun standardProfileDefaultsAreValidAndDoNotRequireGatewayMetadata() {
        assertTrue(FixedEgressMetadata().isValid())
    }

    @Test
    fun customerOwnedFixedEgressWithPublicIpv4IsValid() {
        val metadata = FixedEgressMetadata(
            mode = EgressMode.CUSTOMER_OWNED_FIXED,
            gatewayOwnership = GatewayOwnership.CUSTOMER,
            expectedEgressIpv4 = "8.8.8.8",
            gatewayLabel = "My gateway"
        )

        assertTrue(metadata.validationErrors().toString(), metadata.isValid())
    }

    @Test
    fun fixedEgressRequiresCustomerOwnershipAndExpectedIpv4() {
        assertFalse(
            FixedEgressMetadata(
                mode = EgressMode.CUSTOMER_OWNED_FIXED,
                gatewayOwnership = GatewayOwnership.NONE,
                expectedEgressIpv4 = "8.8.8.8"
            ).isValid()
        )
        assertFalse(
            FixedEgressMetadata(
                mode = EgressMode.CUSTOMER_OWNED_FIXED,
                gatewayOwnership = GatewayOwnership.CUSTOMER
            ).isValid()
        )
    }

    @Test
    fun standardModeRejectsFixedEgressOnlyFields() {
        assertFalse(
            FixedEgressMetadata(
                mode = EgressMode.STANDARD,
                gatewayOwnership = GatewayOwnership.CUSTOMER
            ).isValid()
        )
        assertFalse(
            FixedEgressMetadata(
                mode = EgressMode.STANDARD,
                expectedEgressIpv4 = "8.8.8.8"
            ).isValid()
        )
    }

    @Test
    fun unsupportedSchemaVersionIsRejected() {
        assertFalse(FixedEgressMetadata(schemaVersion = 2).isValid())
    }

    @Test
    fun publicIpv4CandidateAcceptsCanonicalGlobalAddress() {
        assertTrue(FixedEgressMetadata.isPublicIpv4Candidate("1.1.1.1"))
        assertTrue(FixedEgressMetadata.isPublicIpv4Candidate("8.8.8.8"))
        assertTrue(FixedEgressMetadata.isPublicIpv4Candidate("93.184.216.34"))
    }

    @Test
    fun publicIpv4CandidateRejectsPrivateAndSpecialUseRanges() {
        val rejected = listOf(
            "0.1.2.3",
            "10.0.0.1",
            "100.64.0.1",
            "127.0.0.1",
            "169.254.1.1",
            "172.16.0.1",
            "192.0.0.1",
            "192.0.2.1",
            "192.168.1.1",
            "198.18.0.1",
            "198.51.100.1",
            "203.0.113.1",
            "224.0.0.1",
            "240.0.0.1",
            "255.255.255.255"
        )

        rejected.forEach { assertFalse("Unexpectedly accepted $it", FixedEgressMetadata.isPublicIpv4Candidate(it)) }
    }

    @Test
    fun publicIpv4CandidateRejectsMalformedNonCanonicalAndNonIpv4Values() {
        val rejected = listOf(
            "",
            "8.8.8",
            "8.8.8.8.8",
            "256.1.1.1",
            "-1.1.1.1",
            "08.8.8.8",
            "8.8.8.08",
            "8.8.8.8 ",
            "example.com",
            "2001:4860:4860::8888",
            "۸.۸.۸.۸"
        )

        rejected.forEach { assertFalse("Unexpectedly accepted $it", FixedEgressMetadata.isPublicIpv4Candidate(it)) }
    }

    @Test
    fun gatewayLabelMustBeTrimmedBoundedAndFreeOfControlCharacters() {
        assertFalse(
            FixedEgressMetadata(
                mode = EgressMode.CUSTOMER_OWNED_FIXED,
                gatewayOwnership = GatewayOwnership.CUSTOMER,
                expectedEgressIpv4 = "8.8.8.8",
                gatewayLabel = " gateway "
            ).isValid()
        )
        assertFalse(
            FixedEgressMetadata(
                mode = EgressMode.CUSTOMER_OWNED_FIXED,
                gatewayOwnership = GatewayOwnership.CUSTOMER,
                expectedEgressIpv4 = "8.8.8.8",
                gatewayLabel = "bad\nlabel"
            ).isValid()
        )
        assertFalse(
            FixedEgressMetadata(
                mode = EgressMode.CUSTOMER_OWNED_FIXED,
                gatewayOwnership = GatewayOwnership.CUSTOMER,
                expectedEgressIpv4 = "8.8.8.8",
                gatewayLabel = "x".repeat(FixedEgressMetadata.MAX_GATEWAY_LABEL_LENGTH + 1)
            ).isValid()
        )
    }
}
