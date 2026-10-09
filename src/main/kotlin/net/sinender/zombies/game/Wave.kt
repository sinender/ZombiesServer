package net.sinender.zombies.game

data class Wave(
    val waveNumber: Int,
    val zombieCount: Int,
    val zombieHealth: Double,
    val zombieSpeed: Double,
) {
    fun nextWave(): Wave {
        return Wave(
            waveNumber + 1,
            if (waveNumber == 1) 5 else (6 + (waveNumber * 2)),
            zombieHealth * 1.1,
            zombieSpeed * 1.1
        )
    }

    companion object {
        val DEFAULT_WAVE = Wave(1, 1, 20.0, 0.2)
    }
}
