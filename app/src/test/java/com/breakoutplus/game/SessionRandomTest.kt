package com.breakoutplus.game

import org.junit.Assert.*
import org.junit.Test

class SessionRandomTest {
    @Test fun replayAndRestorationUseTheSameStream() {
        val first = SessionRandom(42)
        val second = SessionRandom(42)
        repeat(1000) { assertEquals(first.nextInt(), second.nextInt()) }
        val saved = first.state
        val expected = List(50) { first.nextFloat() }
        val resumed = SessionRandom(0).also { it.state = saved }
        assertEquals(expected, List(50) { resumed.nextFloat() })
    }
    @Test fun lcgHasAStableKnownSequence() {
        val rng = SessionRandom(0)
        assertEquals(-1155484576, rng.nextBits(32))
        assertEquals(-723955400, rng.nextBits(32))
        assertThrows(IllegalArgumentException::class.java) { rng.state = -1 }
    }
    @Test fun simulationTicksAreIndependentOfDisplayRefresh() {
        for (hz in listOf(45, 60, 90, 120, 144, 240)) {
            val clock = FixedStepClock()
            var ticks = 0
            repeat(hz * 10) { ticks += clock.advance(1f / hz) }
            assertEquals("refresh=$hz", 1200, ticks)
            assertEquals(0.0, clock.droppedSeconds, 0.0)
        }
    }
    @Test fun catchupKeepsAllTicksWithinSupportedFrameWindow() {
        val clock = FixedStepClock()
        assertEquals(12, clock.advance(0.1f))
        assertEquals(0, clock.advance(Float.NaN))
        assertEquals(0, clock.advance(-1f))
        clock.reset()
        assertEquals(12, clock.advance(1f))
        assertEquals(0.9, clock.droppedSeconds, 0.000001)
    }
}
