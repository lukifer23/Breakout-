package com.breakoutplus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLayoutPolicyTest {
    @Test
    fun classifyByDp_marksWideSlateCorrectly() {
        val layout = DeviceLayoutPolicy.classifyByDp(widthDp = 960f, heightDp = 720f)
        assertTrue(layout.tabletClass)
        assertTrue(layout.wideSlate)
        assertFalse(layout.largeSlate)
    }

    @Test
    fun classifyByDp_marksLargeSlateCorrectly() {
        val layout = DeviceLayoutPolicy.classifyByDp(widthDp = 1280f, heightDp = 900f)
        assertTrue(layout.tabletClass)
        assertTrue(layout.wideSlate)
        assertTrue(layout.largeSlate)
    }

    @Test
    fun classifyByDp_marksTallFoldAsNonSlate() {
        val layout = DeviceLayoutPolicy.classifyByDp(widthDp = 412f, heightDp = 915f)
        assertFalse(layout.tabletClass)
        assertFalse(layout.wideSlate)
        assertTrue(layout.aspectRatio > 2f)
    }

    @Test
    fun normalizedAspectRatio_handlesInverseOrientation() {
        val portrait = DeviceLayoutPolicy.normalizedAspectRatio(2.2f)
        val landscape = DeviceLayoutPolicy.normalizedAspectRatio(1f / 2.2f)
        assertTrue(kotlin.math.abs(portrait - landscape) < 0.0001f)
    }
}
