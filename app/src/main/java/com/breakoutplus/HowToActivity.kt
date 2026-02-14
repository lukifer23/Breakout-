package com.breakoutplus

import android.os.Bundle
import android.view.View
import com.breakoutplus.databinding.ActivityHowtoBinding

class HowToActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityHowtoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHowtoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonHowToBack.setOnClickListener {
            finish()
            playCloseTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Set up expandable sections
        setupExpandableSection(binding.powerupsHeader, binding.powerupsContent)
        setupExpandableSection(binding.bricksHeader, binding.bricksContent)
        setupExpandableSection(binding.modesHeader, binding.modesContent)

        // Initially collapse all sections
        binding.powerupsContent.visibility = View.GONE
        binding.bricksContent.visibility = View.GONE
        binding.modesContent.visibility = View.GONE

        animateEntry()
    }

    private fun setupExpandableSection(header: View, content: View) {
        header.setOnClickListener {
            if (content.visibility == View.VISIBLE) {
                content.animate().alpha(0f).translationY(-6f).setDuration(UiMotion.OVERLAY_OUT_DURATION).setInterpolator(UiMotion.EMPHASIS_OUT).withEndAction {
                    content.visibility = View.GONE
                    content.alpha = 1f
                    content.translationY = 0f
                }.start()
                (header as android.widget.TextView).text = (header.text as String).replace("▼", "▶")
            } else {
                content.visibility = View.VISIBLE
                content.alpha = 0f
                content.translationY = -6f
                content.animate().alpha(1f).translationY(0f).setDuration(UiMotion.OVERLAY_IN_DURATION).setInterpolator(UiMotion.EMPHASIS_OUT).start()
                (header as android.widget.TextView).text = (header.text as String).replace("▶", "▼")
            }
        }
    }

    private fun animateEntry() {
        val views = listOf(binding.howtoTitle, binding.howtoScroll, binding.howtoFooter)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 18f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(UiMotion.stagger(index, step = 80L))
                .setDuration(UiMotion.ENTRY_DURATION)
                .setInterpolator(UiMotion.EMPHASIS_OUT)
                .start()
        }

        binding.howtoList.post {
            animateStagger(binding.howtoList)
        }
    }

    private fun animateStagger(container: android.view.ViewGroup) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.alpha = 0f
            child.translationY = 14f
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(UiMotion.stagger(i, step = 54L))
                .setDuration(UiMotion.LIST_ITEM_DURATION)
                .setInterpolator(UiMotion.EMPHASIS_OUT)
                .start()
        }
    }
}
