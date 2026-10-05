package com.breakoutplus.game

import kotlin.random.Random

/** Versioned 48-bit LCG; state is explicit so checkpoints resume the exact stream. */
class SessionRandom(seed: Long) : Random() {
    var state: Long = (seed xor MULTIPLIER) and MASK
        set(value) { require(value in 0..MASK); field = value }
    override fun nextBits(bitCount: Int): Int {
        require(bitCount in 0..32)
        if (bitCount == 0) return 0
        state = (state * MULTIPLIER + 11) and MASK
        return (state ushr (48 - bitCount)).toInt()
    }
    companion object {
        const val VERSION = 1
        private const val MULTIPLIER = 0x5DEECE66DL
        private const val MASK = (1L shl 48) - 1
    }
}
