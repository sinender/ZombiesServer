package net.sinender.zombies.game.entities.ai

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.ai.GoalSelector
import net.minestom.server.utils.time.Cooldown
import net.sinender.zombies.game.entities.Zombie
import net.sinender.zombies.utils.RegionUtils.isInRegion
import java.time.Duration
import kotlin.math.min

class WindowGoal(val zombie: Zombie) : GoalSelector(zombie) {
    private var lastBreakTime = 0L
    private var breached = false
    private val window = zombie.window

    override fun shouldStart(): Boolean = !breached

    override fun start() {
        zombie.controller.moveTo(insideTarget())
    }

    override fun tick(time: Long) {
        val breakRegion = window.breakRegion
        val position = entityCreature.position
        if (isInRegion(position.x(), position.y(), position.z(), breakRegion.first, breakRegion.second)) {
            if (!Cooldown.hasCooldown(time, lastBreakTime, Duration.ofSeconds(2))) {
                window.breakWindow(zombie.game, position)
                lastBreakTime = time
            }
        }
        zombie.controller.moveTo(insideTarget())
    }

    override fun shouldEnd(): Boolean {
        val position = entityCreature.position
        val windowRegion = window.windowRegion
        val repairRegion = window.repairRegion
        return isInRegion(position.x(), position.y(), position.z(), windowRegion.first, windowRegion.second) ||
            isInRegion(position.x(), position.y(), position.z(), repairRegion.first, repairRegion.second)
    }

    override fun end() {
        breached = true
    }

    private fun insideTarget(): Pos {
        val first = window.repairRegion.first
        val second = window.repairRegion.second
        val windowFirst = window.windowRegion.first
        val windowSecond = window.windowRegion.second
        return Pos(
            (first.x() + second.x()) / 2.0,
            min(windowFirst.y(), windowSecond.y()),
            (first.z() + second.z()) / 2.0,
        )
    }
}
