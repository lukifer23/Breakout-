package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ModeAccentTest {
    @Test
    fun colorRes_assignsDistinctSurvivalAndTunnelAccents() {
        assertEquals(com.breakoutplus.R.color.bp_orange, ModeAccent.colorRes(GameMode.TUNNEL))
        assertEquals(com.breakoutplus.R.color.bp_flame, ModeAccent.colorRes(GameMode.SURVIVAL))
    }
}
