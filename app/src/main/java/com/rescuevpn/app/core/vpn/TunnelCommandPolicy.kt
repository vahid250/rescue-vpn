package com.rescuevpn.app.core.vpn

/**
 * Commands accepted by the WireGuard service. The service executes them serially;
 * when several commands arrive during one backend transition, only the newest
 * user intent is retained.
 */
internal sealed interface TunnelCommand {
    data class Start(val name: String, val rawConfig: String) : TunnelCommand
    data class Stop(val startId: Int) : TunnelCommand
}

internal object TunnelCommandPolicy {
    fun latest(commands: List<TunnelCommand>): TunnelCommand? = commands.lastOrNull()
}
