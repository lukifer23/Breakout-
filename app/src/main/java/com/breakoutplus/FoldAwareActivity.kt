package com.breakoutplus

import android.graphics.Rect
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
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

    protected fun setFoldAwareRoot(view: View) {
        rootView = view
        basePadding = Rect(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
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
            root.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            return
        }

        val bounds = foldingFeature.bounds
        val hingeSize = if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) bounds.width() else bounds.height()
        if (hingeSize <= 0) {
            root.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            return
        }
        val maxPad = (root.resources.displayMetrics.density * 24f).toInt()
        val extra = minOf(hingeSize / 2, maxPad)
        val newLeft = padding.left + if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) extra else 0
        val newRight = padding.right + if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) extra else 0
        val newTop = padding.top + if (foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL) extra else 0
        val newBottom = padding.bottom + if (foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL) extra else 0
        ViewCompat.setPaddingRelative(root, newLeft, newTop, newRight, newBottom)
    }
}
