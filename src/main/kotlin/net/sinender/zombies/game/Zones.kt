package net.sinender.zombies.game

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.block.Block

enum class Zones(
    val windows: List<Window>,
    val door: Door?
) {
    ENTRANCE(
        windows = listOf(
            Window(
                blockType = Block.SPRUCE_SLAB,
                direction = Vec(-1.0, 0.0, 0.0),
                windowRegion = Pair(Pos(22.0, 8.0, 60.0), Pos(23.0, 6.0, 57.0)),
                zombieSpawn = Pos(17.0, 6.5, 59.0)
            ),
            Window(
                blockType = Block.SPRUCE_SLAB,
                direction = Vec(-1.0, 0.0, 0.0),
                windowRegion = Pair(Pos(24.0, 5.0, 45.0), Pos(25.0, 3.0, 42.0)),
                zombieSpawn = Pos(20.5, 3.5, 44.0)
            ),
            Window(
                blockType = Block.SPRUCE_SLAB,
                direction = Vec(0.0, 0.0, -1.0),
                windowRegion = Pair(Pos(49.0, 5.0, 29.0), Pos(52.0, 3.0, 30.0)),
                zombieSpawn = Pos(51.0, 3.5, 24.5)
            ),
            Window(
                blockType = Block.SPRUCE_SLAB,
                direction = Vec(0.0, 0.0, 1.0),
                windowRegion = Pair(Pos(52.0, 5.0, 60.0), Pos(49.0, 3.0, 61.0)),
                zombieSpawn = Pos(51.0, 3.5, 65.5)
            )
        ),
        door = null
    )

    ;

    class Door(
        val region: Pair<Point, Point>,
        val cost: Int,
    )
}