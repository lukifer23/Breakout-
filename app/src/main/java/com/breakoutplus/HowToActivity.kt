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
        registerSlideCloseOnBackPressed()
        UiMotion.attachPressScale(binding.buttonHowToBack)
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
            val headerLabel = header as android.widget.TextView
            if (content.visibility == View.VISIBLE) {
                headerLabel.text = headerLabel.text.toString().replace("▼", "▶")
                UiMotion.animateExpandableSection(content, expand = false)
            } else {
                headerLabel.text = headerLabel.text.toString().replace("▶", "▼")
                UiMotion.animateExpandableSection(content, expand = true)
            }
        }
    }

    private fun animateEntry() {
        UiMotion.animateScreenSections(
            listOf(binding.howtoTitle, binding.howtoScroll, binding.howtoFooter)
        )
        binding.howtoList.post {
            UiMotion.animateStaggerChildren(binding.howtoList)
        }
    }
}
