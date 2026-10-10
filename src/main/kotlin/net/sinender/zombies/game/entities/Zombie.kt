package net.sinender.zombies.game.entities

import ca.atlasengine.pathfinding.EntityNavigationController
import ca.atlasengine.pathfinding.PathDebugRenderer
import ca.atlasengine.pathfinding.profile.NavigationProfile
import ca.atlasengine.pathfinding.profile.PlatformJumpCapabilities
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.ai.target.ClosestEntityTarget
import net.minestom.server.utils.time.TimeUnit
import net.sinender.zombies.ZombiesServer
import net.sinender.zombies.game.Window
import net.sinender.zombies.game.entities.ai.MeleeAttackGoal
import net.sinender.zombies.game.entities.ai.WindowGoal


class Zombie(val window: Window) : EntityCreature(EntityType.ZOMBIE), AutoCloseable {
    var renderer: PathDebugRenderer = PathDebugRenderer()
    var controller: EntityNavigationController = ZombiesServer.navigation.controller(this, 0.10)
    override fun close() {
        controller.close()
    }

    override fun tick(time: Long) {
        controller.tick()
        renderer.render(controller)
        super.tick(time)
    }
    init {
        addAIGroup(
            listOf(
                WindowGoal(this),
                MeleeAttackGoal(this, 1.6, 20, TimeUnit.SERVER_TICK)
            ),
            listOf(
                ClosestEntityTarget(this, 32.0) { entity -> entity is Player }
            ),
        )
    }

    private companion object {
        const val MIN_DROP = 0.05
        const val MAX_DROP = 1.6
    }
}
