package com.rescuevpn.app

import com.rescuevpn.app.data.model.EgressMode
import com.rescuevpn.app.data.model.FixedEgressMetadata
import com.rescuevpn.app.data.model.FixedEgressMetadataCodec
import com.rescuevpn.app.data.model.GatewayOwnership
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedEgressMetadataCodecTest {
    @Test fun standardMetadataRoundTripsDeterministically() {
        val metadata = FixedEgressMetadata()
        val first = FixedEgressMetadataCodec.encode("legacy8ab", metadata)
        assertEquals(first, FixedEgressMetadataCodec.encode("legacy8ab", metadata))
        assertEquals(metadata, FixedEgressMetadataCodec.decode("legacy8ab", first))
    }

    @Test fun fixedEgressMetadataRoundTripsUnicodeLabel() {
        val metadata = FixedEgressMetadata(
            mode = EgressMode.CUSTOMER_OWNED_FIXED,
            gatewayOwnership = GatewayOwnership.CUSTOMER,
            expectedEgressIpv4 = "8.8.8.8",
            gatewayLabel = "Gateway اروپا"
        )
        val id = "123e4567-e89b-42d3-a456-426614174000"
        assertEquals(metadata, FixedEgressMetadataCodec.decode(id, FixedEgressMetadataCodec.encode(id, metadata)))
    }

    @Test fun recordCannotBeReadUnderAnotherProfileId() {
        val encoded = FixedEgressMetadataCodec.encode("profile-a", FixedEgressMetadata())
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.decode("profile-b", encoded)
        }
    }

    @Test fun unknownFormatVersionIsRejected() {
        val encoded = FixedEgressMetadataCodec.encode("profile-a", FixedEgressMetadata())
            .replace("format_version=1", "format_version=2")
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.decode("profile-a", encoded)
        }
    }

    @Test fun unknownOrDuplicateFieldsAreRejected() {
        val encoded = FixedEgressMetadataCodec.encode("profile-a", FixedEgressMetadata())
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.decode("profile-a", encoded + "\nextra=value")
        }
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.decode("profile-a", encoded + "\nmode=STANDARD")
        }
    }

    @Test fun malformedAndOversizedRecordsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.decode("profile-a", "not-a-record")
        }
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.decode("profile-a", "x".repeat(4097))
        }
    }

    @Test fun invalidMetadataCannotBeEncoded() {
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.encode("profile-a", FixedEgressMetadata(schemaVersion = 99))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.encode(
                "profile-a",
                FixedEgressMetadata(
                    mode = EgressMode.CUSTOMER_OWNED_FIXED,
                    gatewayOwnership = GatewayOwnership.CUSTOMER,
                    expectedEgressIpv4 = "192.168.1.1"
                )
            )
        }
    }

    @Test fun differentProfileIdsProduceDifferentRecords() {
        val metadata = FixedEgressMetadata()
        assertNotEquals(
            FixedEgressMetadataCodec.encode("profile-a", metadata),
            FixedEgressMetadataCodec.encode("profile-b", metadata)
        )
    }

    @Test fun profileIdMustBeBoundedAndNonBlank() {
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.encode(" ", FixedEgressMetadata())
        }
        assertThrows(IllegalArgumentException::class.java) {
            FixedEgressMetadataCodec.encode("x".repeat(129), FixedEgressMetadata())
        }
        assertTrue(FixedEgressMetadataCodec.decode(
            "profile-a", FixedEgressMetadataCodec.encode("profile-a", FixedEgressMetadata())
        ).isValid())
    }
}
