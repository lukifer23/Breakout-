package com.breakoutplus

import android.os.Bundle
import com.breakoutplus.databinding.ActivityPrivacyBinding

class PrivacyActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonPrivacyBack.setOnClickListener { finish() }
        binding.privacyText.text = loadPrivacyText()
        animateEntry()
    }

    private fun loadPrivacyText(): String {
        return resources.openRawResource(R.raw.privacy_policy)
            .bufferedReader()
            .use { it.readText() }
    }

    private fun animateEntry() {
        val views = listOf(binding.privacyTitle, binding.privacyScroll, binding.privacyFooter)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 18f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(80L * index)
                .setDuration(350L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }
}
