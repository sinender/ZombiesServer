package net.sinender.zombies.game.tasks

import net.sinender.zombies.Game

class SpawnZombie(private val game: Game): Runnable {
    override fun run() {
        for (window in game.windows) {
            window.attemptSpawn()
        }
    }
}