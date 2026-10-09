package net.sinender.zombies.instance

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.instance.AddEntityToInstanceEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.world.DimensionType
import java.nio.file.Path

object Lobby {
    val SPAWN_POINT: Pos = Pos(0.5, 67.0, 0.5, 0f, 0f)
    val INSTANCE: Instance = createLobbyInstance()

    private fun createLobbyInstance(): Instance {
        val instance = MinecraftServer.getInstanceManager().createInstanceContainer(
            AnvilLoader(Path.of("lobby"), DimensionType.OVERWORLD.key())
        )

        instance.defaultClock()?.pause()
        instance.time = 6000

        instance.eventNode().addListener(AddEntityToInstanceEvent::class.java) { event ->
            val player = event.entity as? Player ?: return@addListener
            onJoin(player)
        }.addListener(PlayerMoveEvent::class.java) { event ->
            val player = event.player
            if (player.position.y() < 0) {
                player.teleport(SPAWN_POINT)
            }
        }.addListener(PlayerBlockBreakEvent::class.java) { event ->
            event.isCancelled = true
        }

        return instance
    }

    private fun onJoin(player: Player) {
    }
}
