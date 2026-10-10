package net.sinender.zombies

import it.unimi.dsi.fastutil.Pair
import net.hollowcube.schem.reader.SchematicReader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.Music.GAME
import net.minestom.server.tag.Tag
import net.minestom.server.timer.ExecutionType
import net.minestom.server.timer.TaskSchedule
import net.sinender.zombies.Queue.Companion.GAME_START_DELAY
import net.sinender.zombies.ZombiesServer.navigation
import net.sinender.zombies.game.Wave
import net.sinender.zombies.game.Window
import net.sinender.zombies.game.tasks.SpawnZombie
import net.sinender.zombies.game.tasks.Tick
import net.sinender.zombies.instance.Lobby
import net.sinender.zombies.instance.Lobby.SPAWN_POINT
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class Game(players: Set<UUID>) : PacketGroupingAudience {
    val instance: InstanceContainer = createGameInstance()
    val playerList = ArrayList<Player>()
    val windows = ArrayList<Window>()
    val ending = AtomicBoolean(false)

    var currentWave: Wave
    var zombiesRemaining: Int = 0

    init {
        navigation.watchBlockChanges(instance);
        windows.add(
            Window(
                this,
                Block.SPRUCE_SLAB,
                Pair.of(Pos(5.0, 16.0, 43.0), Pos(6.0, 14.0, 41.0)),
                Pair.of(Pos(4.0, 16.0, 43.0), Pos(5.0, 14.5, 41.0)),
                Pair.of(Pos(6.0, 12.0, 43.0), Pos(5.0, 17.0, 41.0)),
                Pos(2.0, 14.5, 42.0),
            )
        )

        for (uuid in players) {
            val player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid) ?: continue

            playerList.add(player)
            player.setTag(GAME, this)

            player.setInstance(instance, SPAWN_POINT)
        }

        GAMES.add(this)

        currentWave = Wave.DEFAULT_WAVE
        val counter = AtomicInteger(5)
        MinecraftServer.getSchedulerManager().submitTask({
            val time = counter.decrementAndGet()
            if (time <= 0) {
                sendMessage(Component.text("The game has started!", NamedTextColor.GREEN))
                zombiesRemaining = currentWave.zombieCount
                return@submitTask TaskSchedule.stop()
            }

            sendMessage(Component.text("Game starting in $time seconds...", NamedTextColor.YELLOW))
            TaskSchedule.seconds(1)
        }, ExecutionType.TICK_END)

        MinecraftServer.getSchedulerManager().buildTask(Tick(this)).repeat(Duration.ofMillis(50)).schedule()
        MinecraftServer.getSchedulerManager().buildTask(SpawnZombie(this)).repeat(Duration.ofSeconds(1)).schedule()
    }

    fun onGameEnd() {
        ending.set(true)

        for (player in playerList) {
            player.setInstance(Lobby.INSTANCE, Lobby.SPAWN_POINT)
            player.removeTag(GAME)
        }

        playerList.clear()
        GAMES.remove(this)
    }

    fun onDisconnect(player: Player) {
        playerList.remove(player)

        sendMessage(playerHasLeft(player.username))
    }

    override fun getPlayers(): Collection<Player> = Collections.unmodifiableCollection(playerList)

    companion object {
        val SPAWN_POINT = Pos(29.5, 10.0, 28.5, 0f, 0f)
        val GAME: Tag<Game> = Tag.Transient("Game")

        private val GAMES = HashSet<Game>()

        private val playerHasLeft: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" ", NamedTextColor.GRAY),
                Component.text(username, NamedTextColor.GRAY),
                Component.text(" has left the game!", NamedTextColor.GRAY),
            )
        }

        private fun createGameInstance(): InstanceContainer {
            val instance = MinecraftServer.getInstanceManager().createInstanceContainer()

            for (i in 0 until 5) {
                for (j in 0 until 5) {
                    instance.loadChunk(i, j)
                }
            }

            try {
                val schem = Files.readAllBytes(Path.of("worlds/zombies.schem"))
                val schematic = SchematicReader.sponge().read(schem)
                val batch = schematic.createBatch()
                batch.apply(instance) { }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            instance.defaultClock()?.pause()
            instance.time = 6000

            return instance
        }
    }
}
