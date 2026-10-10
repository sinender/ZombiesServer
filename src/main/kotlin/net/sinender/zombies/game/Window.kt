package net.sinender.zombies.game

import it.unimi.dsi.fastutil.Pair
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.WorldEventPacket
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.PacketSendingUtils
import net.minestom.server.worldevent.WorldEvent
import net.sinender.zombies.Game
import net.sinender.zombies.game.entities.Zombie
import kotlin.math.max
import kotlin.math.min

data class Window(
    val game: Game,
    val blockType: Block,
    val windowRegion: Pair<Point, Point>,
    val breakRegion: Pair<Point, Point>,
    val repairRegion: Pair<Point, Point>,
    val zombieSpawn: Pos,
) {
    val rebuildBlocks = mutableListOf<Block>()
    fun attemptSpawn() {
        if (game.zombiesRemaining > 0) {
            game.zombiesRemaining--

            val zombie = Zombie(this)
            zombie.setInstance(game.instance, zombieSpawn)
        }
    }

    fun breakWindow(zombiePos: Point) {
        val first = windowRegion.first()
        val second = windowRegion.second()
        val minX = min(first.x(), second.x()).toInt()
        val maxX = max(first.x(), second.x()).toInt()
        val minY = min(first.y(), second.y()).toInt()
        val maxY = max(first.y(), second.y()).toInt()
        val minZ = min(first.z(), second.z()).toInt()
        val maxZ = max(first.z(), second.z()).toInt()

        for (y in minY..maxY) {
            var closestBlock: Point? = null
            var closestDistance = Double.MAX_VALUE
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val blockPos: Point = BlockVec(x.toDouble(), y.toDouble(), z.toDouble())
                    if (game.instance.getBlock(blockPos).key() == blockType.key()) {
                        val distance = blockPos.distance(zombiePos)
                        if (distance < closestDistance) {
                            closestDistance = distance
                            closestBlock = blockPos
                        }
                    }
                }
            }
            if (closestBlock != null) {
                val block = game.instance.getBlock(closestBlock)
                rebuildBlocks.add(block)
                game.instance.setBlock(closestBlock, Block.AIR)
                val chunk: Chunk? = game.instance.getChunkAt(closestBlock)
                chunk?.sendPacketToViewers(
                    WorldEventPacket(
                        WorldEvent.PARTICLES_DESTROY_BLOCK.id(),
                        closestBlock,
                        block.stateId(),
                        false
                    )
                )
                chunk?.viewersAsAudience?.playSound(Sound.sound(SoundEvent.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.key(), Sound.Source.HOSTILE, 1f, 1f))
                break
            }
        }
    }
}
