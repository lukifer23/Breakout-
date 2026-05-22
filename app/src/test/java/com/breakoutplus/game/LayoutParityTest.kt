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
