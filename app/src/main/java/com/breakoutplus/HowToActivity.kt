package com.breakoutplus

import android.os.Bundle
import com.breakoutplus.databinding.ActivityHowtoBinding

class HowToActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityHowtoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHowtoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonHowToBack.setOnClickListener { finish() }
    }
}
