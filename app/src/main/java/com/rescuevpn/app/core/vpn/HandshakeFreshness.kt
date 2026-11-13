package com.rescuevpn.app.core.vpn

/**
 * A recent handshake is evidence of recent peer activity, not proof that general
 * internet access works. Keep this signal separate from interface UP state.
 */
internal object HandshakeFreshness {
    const val DEFAULT_MAX_AGE_MILLIS = 180_000L

    fun isRecent(
        handshakeMillis: Long,
        nowMillis: Long,
        maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS
    ): Boolean {
        if (handshakeMillis <= 0L || maxAgeMillis < 0L) return false
        val age = nowMillis - handshakeMillis
        return age in 0L..maxAgeMillis
    }

    fun ageSeconds(handshakeMillis: Long, nowMillis: Long): Long? {
        if (handshakeMillis <= 0L || handshakeMillis > nowMillis) return null
        return (nowMillis - handshakeMillis) / 1000L
    }
}
