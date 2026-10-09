package net.sinender.zombies.game.entities.ai

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.ai.GoalSelector
import net.minestom.server.utils.time.Cooldown
import net.sinender.zombies.game.Window
import java.time.Duration

class WindowGoal(entityCreature: EntityCreature, private val window: Window) : GoalSelector(entityCreature) {
    private var lastBreakTime = 0L

    override fun shouldStart(): Boolean {
        val first = window.spawnRegion.first()
        val second = window.spawnRegion.second()
        val x = entityCreature.position.x()
        val y = entityCreature.position.y()
        val z = entityCreature.position.z()
        return x >= first.x() && x <= second.x() &&
            y >= first.y() && y <= second.y() &&
            z >= first.z() && z <= second.z()
    }

    override fun start() {
        val first = window.repairRegion.first()
        val second = window.repairRegion.second()
        val x = (first.x() + second.x()) / 2
        val y = first.y()
        val z = (first.z() + second.z()) / 2
        entityCreature.navigator.setPathTo(Pos(x, y, z))
    }

    override fun tick(time: Long) {
        if (!Cooldown.hasCooldown(time, lastBreakTime, Duration.ofSeconds(2))) {
            val first = window.spawnRegion.first()
            val second = window.spawnRegion.second()
            val x = entityCreature.position.x()
            val y = entityCreature.position.y()
            val z = entityCreature.position.z()
            if (x >= first.x() && x <= second.x() &&
                y >= first.y() && y <= second.y() &&
                z >= first.z() && z <= second.z()
            ) {
                window.breakWindow(entityCreature.position)
                lastBreakTime = time
            }
        }
    }

    override fun shouldEnd(): Boolean {
        val first = window.spawnRegion.first()
        val second = window.spawnRegion.second()
        val x = entityCreature.position.x()
        val y = entityCreature.position.y()
        val z = entityCreature.position.z()
        return x < first.x() || x > second.x() ||
            y < first.y() || y > second.y() ||
            z < first.z() || z > second.z()
    }

    override fun end() {
    }
}
