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

        binding.buttonHowToBack.setOnClickListener { finish() }

        // Set up expandable sections
        setupExpandableSection(binding.powerupsHeader, binding.powerupsContent)
        setupExpandableSection(binding.bricksHeader, binding.bricksContent)
        setupExpandableSection(binding.modesHeader, binding.modesContent)

        // Initially collapse all sections
        binding.powerupsContent.visibility = View.GONE
        binding.bricksContent.visibility = View.GONE
        binding.modesContent.visibility = View.GONE
    }

    private fun setupExpandableSection(header: View, content: View) {
        header.setOnClickListener {
            if (content.visibility == View.VISIBLE) {
                content.animate().alpha(0f).setDuration(200).withEndAction {
                    content.visibility = View.GONE
                    content.alpha = 1f
                }.start()
                (header as android.widget.TextView).text = (header.text as String).replace("▼", "▶")
            } else {
                content.visibility = View.VISIBLE
                content.alpha = 0f
                content.animate().alpha(1f).setDuration(200).start()
                (header as android.widget.TextView).text = (header.text as String).replace("▶", "▼")
            }
        }
    }
}
