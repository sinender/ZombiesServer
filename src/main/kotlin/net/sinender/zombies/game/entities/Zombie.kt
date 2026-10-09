package net.sinender.zombies.game.entities

import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.ai.goal.MeleeAttackGoal
import net.minestom.server.entity.ai.target.ClosestEntityTarget
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget
import net.minestom.server.utils.time.TimeUnit
import net.sinender.zombies.game.Window
import net.sinender.zombies.game.entities.ai.WindowGoal

class Zombie(window: Window) : EntityCreature(EntityType.ZOMBIE) {
    init {
        addAIGroup(
            listOf(
                WindowGoal(this, window),
                MeleeAttackGoal(this, 1.6, 20, TimeUnit.SERVER_TICK)
            ),
            listOf(
                LastEntityDamagerTarget(this, 32f),
                ClosestEntityTarget(this, 1000.0) { entity -> entity is Player },
            ),
        )
    }
}
