package net.sinender.zombies.game

import it.unimi.dsi.fastutil.Pair
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.block.Block
import net.sinender.zombies.Game
import net.sinender.zombies.game.entities.Zombie
import kotlin.math.max
import kotlin.math.min

data class Window(
    val game: Game,
    val blockType: Block,
    val windowRegion: Pair<Point, Point>,
    val spawnRegion: Pair<Point, Point>,
    val repairRegion: Pair<Point, Point>,
    val zombieSpawn: Pos,
) {

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
                    val blockPos: Point = Pos(x.toDouble(), y.toDouble(), z.toDouble())
                    if (game.instance.getBlock(blockPos).stateId() == blockType.stateId()) {
                        val distance = blockPos.distance(zombiePos)
                        if (distance < closestDistance) {
                            closestDistance = distance
                            closestBlock = blockPos
                        }
                    }
                }
            }
            if (closestBlock != null) {
                game.instance.setBlock(closestBlock, Block.AIR)
                break
            }
        }
    }
}
