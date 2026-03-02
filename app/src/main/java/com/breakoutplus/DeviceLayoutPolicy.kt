package com.breakoutplus

import kotlin.math.max

data class DeviceLayoutClass(
    val shortDp: Float,
    val longDp: Float,
    val aspectRatio: Float,
    val tabletClass: Boolean,
    val wideSlate: Boolean,
    val largeSlate: Boolean
)

object DeviceLayoutPolicy {
    private const val TABLET_SHORT_DP = 600f
    private const val LARGE_TABLET_SHORT_DP = 840f
    private const val SLATE_ASPECT_MAX = 1.85f

    fun classifyByDp(widthDp: Float, heightDp: Float): DeviceLayoutClass {
        val shortDp = minOf(widthDp, heightDp)
        val longDp = maxOf(widthDp, heightDp)
        val aspect = (longDp / shortDp.coerceAtLeast(1f)).coerceAtLeast(1f)
        val tabletClass = shortDp >= TABLET_SHORT_DP
        val wideSlate = tabletClass && aspect <= SLATE_ASPECT_MAX
        val largeSlate = wideSlate && shortDp >= LARGE_TABLET_SHORT_DP
        return DeviceLayoutClass(
            shortDp = shortDp,
            longDp = longDp,
            aspectRatio = aspect,
            tabletClass = tabletClass,
            wideSlate = wideSlate,
            largeSlate = largeSlate
        )
    }

    fun normalizedAspectRatio(aspectRatio: Float): Float {
        return max(aspectRatio, 1f / aspectRatio.coerceAtLeast(0.0001f))
    }

    fun isSlateAspect(aspectRatio: Float): Boolean {
        return normalizedAspectRatio(aspectRatio) <= SLATE_ASPECT_MAX
    }
}