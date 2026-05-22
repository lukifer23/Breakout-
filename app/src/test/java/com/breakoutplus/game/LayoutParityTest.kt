package com.breakoutplus.game

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LayoutParityTest {
    @Test
    fun gameLayout_includesGodSkipControlAcrossDeviceBuckets() {
        val layoutPaths = listOf(
            "src/main/res/layout/activity_game.xml",
            "src/main/res/layout-sw600dp/activity_game.xml",
            "src/main/res/layout-sw720dp/activity_game.xml"
        )
        layoutPaths.forEach { path ->
            val xml = readLayout(path)
            assertTrue(
                "Missing buttonSkipLevel in $path",
                xml.contains("android:id=\"@+id/buttonSkipLevel\"")
            )
        }
    }

    @Test
    fun gameLayout_includesOverlayGlassPanelsAcrossDeviceBuckets() {
        val layoutPaths = listOf(
            "src/main/res/layout/activity_game.xml",
            "src/main/res/layout-sw600dp/activity_game.xml",
            "src/main/res/layout-sw720dp/activity_game.xml"
        )
        layoutPaths.forEach { path ->
            val xml = readLayout(path)
            assertTrue(
                "Missing hud_glass_panel_elevated in $path",
                xml.contains("@drawable/hud_glass_panel_elevated")
            )
        }
    }

    @Test
    fun gameLayout_usesHudShieldTrackColorOnTablets() {
        listOf(
            "src/main/res/layout-sw600dp/activity_game.xml",
            "src/main/res/layout-sw720dp/activity_game.xml"
        ).forEach { path ->
            val xml = readLayout(path)
            assertTrue(
                "Shield track should use HUD glass fill in $path",
                xml.contains("app:trackColor=\"@color/bp_hud_glass_fill_deep\"")
            )
        }
    }

    @Test
    fun modeLayoutPolicy_slateRowBoosts_exceedPhoneDensity() {
        val slateVolley = ModeLayoutPolicy.volleyRowBoost(aspectRatio = 1.55f, isSlate = true, levelIndex = 0)
        val phoneVolley = ModeLayoutPolicy.volleyRowBoost(aspectRatio = 1.55f, isSlate = false, levelIndex = 0)
        val slateTunnel = ModeLayoutPolicy.tunnelRowBoost(aspectRatio = 1.55f, isSlate = true, levelIndex = 0)
        val phoneTunnel = ModeLayoutPolicy.tunnelRowBoost(aspectRatio = 1.95f, isSlate = false, levelIndex = 0)

        assertTrue(slateVolley > phoneVolley)
        assertTrue(slateTunnel > phoneTunnel)
    }

    private fun readLayout(relativePath: String): String {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = candidates.firstOrNull { it.exists() }
        assertTrue("Unable to locate layout file: $relativePath", file != null)
        return file!!.readText()
    }
}
