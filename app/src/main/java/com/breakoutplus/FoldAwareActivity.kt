package com.breakoutplus

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class FoldAwareActivity : AppCompatActivity() {
    private var rootView: View? = null
    private var basePadding: Rect? = null
    private var layoutJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply night mode early so activities are created with the correct theme.
        val settings = SettingsManager.load(this)
        AppCompatDelegate.setDefaultNightMode(
            if (settings.darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
    }

    protected fun setFoldAwareRoot(view: View) {
        rootView = view
        basePadding = Rect(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
    }

    protected fun playOpenTransition(enterAnim: Int, exitAnim: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(enterAnim, exitAnim)
        }
    }

    protected fun playCloseTransition(enterAnim: Int, exitAnim: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(enterAnim, exitAnim)
        }
    }

    override fun onStart() {
        super.onStart()
        val root = rootView ?: return
        layoutJob?.cancel()
        layoutJob = lifecycleScope.launch {
            WindowInfoTracker.getOrCreate(this@FoldAwareActivity)
                .windowLayoutInfo(this@FoldAwareActivity)
                .collect { info -> applyFoldingPadding(root, info) }
        }
    }

    override fun onStop() {
        layoutJob?.cancel()
        layoutJob = null
        super.onStop()
    }

    private fun applyFoldingPadding(root: View, info: WindowLayoutInfo) {
        val padding = basePadding ?: Rect(0, 0, 0, 0)
        val foldingFeature = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
        if (foldingFeature == null || !foldingFeature.isSeparating) {
            setPaddingIfChanged(root, padding.left, padding.top, padding.right, padding.bottom)
            return
        }

        val bounds = foldingFeature.bounds
        val hingeSize = if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) bounds.width() else bounds.height()
        if (hingeSize <= 0) {
            setPaddingIfChanged(root, padding.left, padding.top, padding.right, padding.bottom)
            return
        }
        val maxPad = (root.resources.displayMetrics.density * 24f).toInt()
        val extra = minOf(hingeSize / 2, maxPad)
        val newLeft = padding.left + if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) extra else 0
        val newRight = padding.right + if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) extra else 0
        val newTop = padding.top + if (foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL) extra else 0
        val newBottom = padding.bottom + if (foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL) extra else 0
        setPaddingIfChanged(root, newLeft, newTop, newRight, newBottom)
    }

    private fun setPaddingIfChanged(root: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (
            root.paddingLeft == left &&
            root.paddingTop == top &&
            root.paddingRight == right &&
            root.paddingBottom == bottom
        ) {
            return
        }
        root.setPadding(left, top, right, bottom)
    }
}
