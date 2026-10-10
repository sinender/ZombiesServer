package net.sinender.zombies.game.entities.ai

import ca.atlasengine.pathfinding.profile.NavigationModifiers
import ca.atlasengine.pathfinding.profile.PlatformJumpCapabilities
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.ai.GoalSelector
import net.minestom.server.utils.time.Cooldown
import net.sinender.zombies.game.entities.Zombie
import java.time.Duration
import java.time.temporal.TemporalUnit

class MeleeAttackGoal : GoalSelector {
    private var lastHit = 0L
    private val range: Double
    private val delay: Duration
    private var stop = false
    private var cachedTarget: Entity? = null
    private val zombie: Zombie

    constructor(
        entityCreature: EntityCreature,
        range: Double,
        delay: Int,
        timeUnit: TemporalUnit,
    ) : this(entityCreature, range, Duration.of(delay.toLong(), timeUnit))

    constructor(
        entityCreature: EntityCreature,
        range: Double,
        delay: Duration,
    ) : super(entityCreature) {
        this.range = range
        this.delay = delay
        this.zombie = entityCreature as Zombie
    }

    override fun shouldStart(): Boolean {
        cachedTarget = findTarget()
        return cachedTarget != null
    }

    override fun start() {
        val target = cachedTarget ?: return
        entityCreature.navigator.setPathTo(target.position)
    }

    override fun tick(time: Long) {
        val target = cachedTarget ?: findTarget()
        cachedTarget = null
        if (target == null || target.isRemoved) {
            stop = true
            return
        }
        stop = false

        entityCreature.lookAt(target)
        if (entityCreature.getDistanceSquared(target) <= range * range) {
            if (!Cooldown.hasCooldown(time, lastHit, delay)) {
                entityCreature.attack(target, true)
                lastHit = time
            }
            return
        }

        zombie.controller.moveTo(target.position)
    }

    override fun shouldEnd(): Boolean = stop

    override fun end() {
    }
}
