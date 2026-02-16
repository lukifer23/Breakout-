package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportStateTest {

    @Test
    fun reapply_doesNothingWithoutValidViewport() {
        val state = ViewportState()
        var calls = 0

        state.reapply { _, _ -> calls += 1 }

        assertEquals(0, calls)
        assertFalse(state.hasViewport())
    }

    @Test
    fun update_ignoresInvalidDimensionsAndKeepsPreviousViewport() {
        val state = ViewportState()
        state.update(1200, 2600)
        state.update(0, 1000)
        state.update(1000, -1)

        var width = 0
        var height = 0
        state.reapply { w, h ->
            width = w
            height = h
        }

        assertTrue(state.hasViewport())
        assertEquals(1200, width)
        assertEquals(2600, height)
    }

    @Test
    fun reapply_usesLatestViewport() {
        val state = ViewportState()
        state.update(1080, 2400)
        state.update(1848, 2208)

        var width = 0
        var height = 0
        state.reapply { w, h ->
            width = w
            height = h
        }

        assertEquals(1848, width)
        assertEquals(2208, height)
    }
}

