package com.breakoutplus.game

data class PowerupStatus(
    val type: PowerUpType,
    val remainingSeconds: Int,
    val charges: Int = 0
)
