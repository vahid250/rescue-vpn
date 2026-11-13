package com.rescuevpn.app

import com.rescuevpn.app.data.model.ProfileIdGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ProfileIdGeneratorTest {

    @Test
    fun newProfileIdsAreCanonicalFullUuids() {
        val id = ProfileIdGenerator.newId()
        assertEquals(36, id.length)
        assertEquals(id, UUID.fromString(id).toString())
        assertTrue(id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")))
    }

    @Test
    fun generatedIdsAreNotTruncatedToEightCharacters() {
        val first = ProfileIdGenerator.newId()
        val second = ProfileIdGenerator.newId()
        assertNotEquals(first, second)
        assertNotEquals(first.take(8), second.take(8))
    }
}
