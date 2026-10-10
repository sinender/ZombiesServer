package net.sinender.zombies.game.entities

import ca.atlasengine.pathfinding.EntityNavigationController
import ca.atlasengine.pathfinding.PathDebugRenderer
import ca.atlasengine.pathfinding.profile.NavigationProfile
import ca.atlasengine.pathfinding.profile.PlatformJumpCapabilities
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.ai.target.ClosestEntityTarget
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget
import net.minestom.server.entity.metadata.monster.zombie.ZombieMeta
import net.minestom.server.utils.time.TimeUnit
import net.sinender.zombies.Game
import net.sinender.zombies.ZombiesServer
import net.sinender.zombies.game.Wave
import net.sinender.zombies.game.Window
import net.sinender.zombies.game.entities.ai.MeleeAttackGoal
import net.sinender.zombies.game.entities.ai.WindowGoal


class Zombie(val game: Game, val window: Window, wave: Wave) : EntityCreature(EntityType.ZOMBIE), AutoCloseable {
    var controller: EntityNavigationController = ZombiesServer.navigation.controller(this, wave.zombieSpeed)

    override fun close() {
        controller.close()
    }

    override fun tick(time: Long) {
        controller.tick()
        super.tick(time)
    }

    init {
        addAIGroup(
            listOf(
                WindowGoal(this),
                MeleeAttackGoal(this, 1.6, 20, TimeUnit.SERVER_TICK)
            ),
            listOf(
                LastEntityDamagerTarget(this, 32f),
                ClosestEntityTarget(this, 1000.0) { entity -> entity is Player && entity.gameMode != GameMode.CREATIVE },
            ),
        )
    }
}
