package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.breakoutplus.FoldAwareActivity

class SplashActivity : FoldAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.splashRoot)
        setFoldAwareRoot(rootView)

        // Animate title elements
        val titleText = findViewById<android.widget.TextView>(R.id.splashTitle)
        val subtitleText = findViewById<android.widget.TextView>(R.id.splashSubtitle)

        titleText.alpha = 0f
        titleText.scaleX = 0.8f
        titleText.scaleY = 0.8f
        subtitleText.alpha = 0f

        titleText.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(200)
            .start()

        subtitleText.animate()
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(500)
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }
}
