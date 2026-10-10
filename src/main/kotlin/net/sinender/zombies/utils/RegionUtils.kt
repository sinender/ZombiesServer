package net.sinender.zombies.utils

import it.unimi.dsi.fastutil.Pair
import net.minestom.server.coordinate.Point

object RegionUtils {
    fun isInRegion(x: Double, y: Double, z: Double, first: Point, second: Point): Boolean {
        val minX = minOf(first.x(), second.x())
        val maxX = maxOf(first.x(), second.x())
        val minY = minOf(first.y(), second.y())
        val maxY = maxOf(first.y(), second.y())
        val minZ = minOf(first.z(), second.z())
        val maxZ = maxOf(first.z(), second.z())
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }
}