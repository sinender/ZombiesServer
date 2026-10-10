package net.sinender.zombies.game.tasks

import net.sinender.zombies.Game
import net.sinender.zombies.utils.RegionUtils

class RebuildBarrier(val game: Game) : Runnable {
    override fun run() {
        if (game.ending.get()) return
        if (!game.started.get()) return
        game.players.forEach { player ->
            if (!player.isSneaking) return@forEach
            game.windows.forEach { window ->
                val pos = player.position
                if (RegionUtils.isInRegion(
                        pos.x(),
                        pos.y(),
                        pos.z(),
                        window.repairRegion.first,
                        window.repairRegion.second
                    )
                ) {
                    window.rebuildWindow(game)
                }
            }
        }
    }
}