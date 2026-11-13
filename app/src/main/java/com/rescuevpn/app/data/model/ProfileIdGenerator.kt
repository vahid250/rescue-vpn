package com.rescuevpn.app.data.model

import java.util.UUID

/**
 * Generates collision-resistant identifiers for newly imported profiles.
 * Existing persisted identifiers are intentionally left unchanged.
 */
internal object ProfileIdGenerator {
    fun newId(): String = UUID.randomUUID().toString()
}
