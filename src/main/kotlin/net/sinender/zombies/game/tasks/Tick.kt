package net.sinender.zombies.game.tasks

import net.sinender.zombies.Game

class Tick(private val game: Game): Runnable {
    override fun run() {
        if (game.ending.get()) return

//        if (game.zombiesRemaining <= 0) {
//            game.currentWave = game.currentWave.nextWave()
//            game.zombiesRemaining = game.currentWave.zombieCount
//        }
    }
}