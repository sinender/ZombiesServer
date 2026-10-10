package net.sinender.zombies.game

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.WorldEventPacket
import net.minestom.server.sound.SoundEvent
import net.minestom.server.worldevent.WorldEvent
import net.sinender.zombies.Game
import net.sinender.zombies.game.entities.Zombie
import kotlin.math.max
import kotlin.math.min

data class Window(
    val blockType: Block,
    val direction: Vec,
    val windowRegion: Pair<Point, Point>,
    val zombieSpawn: Pos,
) {
    val breakRegion = Pair(
        Pos(windowRegion.first.x() + direction.x(), windowRegion.first.y(), windowRegion.first.z() + direction.z()),
        Pos(windowRegion.second.x() + direction.x(), windowRegion.second.y(), windowRegion.second.z() + direction.z())
    )
    val repairRegion = Pair(
        Pos(windowRegion.first.x() - direction.x(), windowRegion.first.y() - 1, windowRegion.first.z() - direction.z()),
        Pos(windowRegion.second.x() - direction.x(), windowRegion.second.y() - 1, windowRegion.second.z() - direction.z())
    )

    val rebuildBlocks = mutableListOf<Pair<Block, Point>>()

    fun attemptSpawn(game: Game) {
        if (game.zombiesSpawned < game.currentWave.zombieCount) {
            game.zombiesSpawned++

            val zombie = Zombie(game, this, game.currentWave)
            zombie.setInstance(game.instance, zombieSpawn)
        }
    }

    fun breakWindow(game: Game, zombiePos: Point) {
        val first = windowRegion.first
        val second = windowRegion.second
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
                rebuildBlocks.add(Pair(block, closestBlock))
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
                chunk?.viewersAsAudience?.playSound(Sound.sound(SoundEvent.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.key(), Sound.Source.HOSTILE, 1f, 1f), closestBlock.x(), closestBlock.y(), closestBlock.z())
                break
            }
        }
    }

    fun rebuildWindow(game: Game) {
        if (game.instance.getNearbyEntities(breakRegion.second, 2.0).any { it is Zombie }) return
        val block = rebuildBlocks.removeFirstOrNull()
        if (block != null) {
            val (blockData, blockPos) = block
            game.instance.setBlock(blockPos, blockData)
            val chunk: Chunk? = game.instance.getChunkAt(blockPos)
            chunk?.sendPacketToViewers(
                WorldEventPacket(
                    WorldEvent.PARTICLES_DESTROY_BLOCK.id(),
                    blockPos,
                    blockData.stateId(),
                    false
                )
            )
            chunk?.viewersAsAudience?.playSound(Sound.sound(SoundEvent.BLOCK_WOOD_PLACE.key(), Sound.Source.BLOCK, 1f, 1f), blockPos.x(), blockPos.y(), blockPos.z())
        }
    }
}
