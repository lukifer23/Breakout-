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

        binding.buttonPrivacyBack.setOnClickListener {
            finish()
            playCloseTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        registerSlideCloseOnBackPressed()
        UiMotion.attachPressScale(binding.buttonPrivacyBack)
        binding.privacyText.text = loadPrivacyText()
        animateEntry()
    }

    private fun loadPrivacyText(): String {
        return resources.openRawResource(R.raw.privacy_policy)
            .bufferedReader()
            .use { it.readText() }
    }

    private fun animateEntry() {
        UiMotion.animateScreenSections(
            listOf(binding.privacyTitle, binding.privacyScroll, binding.privacyFooter)
        )
    }
}
