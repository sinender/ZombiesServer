package net.sinender.zombies.game.tasks

import net.sinender.zombies.Game

class SpawnZombie(private val game: Game) : Runnable {
    override fun run() {
        if (game.ending.get()) return
        if (!game.started.get()) return
        val window = game.windows.randomOrNull() ?: return
        window.attemptSpawn(game)
    }
}