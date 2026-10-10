package net.sinender.zombies

import ca.atlasengine.pathfinding.NavigationSystem
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.sinender.zombies.instance.BlockHandlers
import net.sinender.zombies.instance.Lobby

object ZombiesServer {
    lateinit var navigation: NavigationSystem
    @JvmStatic
    fun main(args: Array<String>) {
        val auth: Auth = Auth.Online()
        val minecraftServer = MinecraftServer.init(auth)

        BlockHandlers.register(MinecraftServer.getBlockManager())

        val queues = Queue.Manager()

        Queue.Commands.register(queues, MinecraftServer.getCommandManager())

        val events = MinecraftServer.getGlobalEventHandler()

        navigation = NavigationSystem.create()

        events.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
            val player: Player = event.player

            event.spawningInstance = Lobby.INSTANCE
            player.respawnPoint = Lobby.SPAWN_POINT
        }

        events.addListener(PlayerDisconnectEvent::class.java) { event ->
            val player: Player = event.player

            queues.dequeue(player)

            val game = player.getTag(Game.GAME)
            if (game != null) game.onDisconnect(player)
        }

        minecraftServer.start("0.0.0.0", 25565)
    }
}
