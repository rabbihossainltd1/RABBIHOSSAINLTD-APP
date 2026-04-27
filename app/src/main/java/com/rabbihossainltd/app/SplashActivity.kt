package com.rabbihossainltd.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.rabbihossainltd.app.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val SPLASH_DURATION = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide system UI for immersive experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        startSplashAnimations()
    }

    private fun startSplashAnimations() {
        // Initial state - everything invisible/scaled down
        binding.logoContainer.alpha = 0f
        binding.logoContainer.scaleX = 0.3f
        binding.logoContainer.scaleY = 0.3f
        binding.appNameText.alpha = 0f
        binding.appNameText.translationY = 40f
        binding.subtitleText.alpha = 0f
        binding.subtitleText.translationY = 30f
        binding.glowCircle1.alpha = 0f
        binding.glowCircle2.alpha = 0f
        binding.loadingDots.alpha = 0f
        binding.versionText.alpha = 0f

        // Step 1: Animate logo in with overshoot
        val logoScaleX = ObjectAnimator.ofFloat(binding.logoContainer, "scaleX", 0.3f, 1.1f, 1.0f)
        val logoScaleY = ObjectAnimator.ofFloat(binding.logoContainer, "scaleY", 0.3f, 1.1f, 1.0f)
        val logoAlpha = ObjectAnimator.ofFloat(binding.logoContainer, "alpha", 0f, 1f)
        logoScaleX.duration = 700
        logoScaleY.duration = 700
        logoAlpha.duration = 500
        logoScaleX.interpolator = OvershootInterpolator(2.0f)
        logoScaleY.interpolator = OvershootInterpolator(2.0f)

        val logoSet = AnimatorSet()
        logoSet.playTogether(logoScaleX, logoScaleY, logoAlpha)
        logoSet.start()

        // Step 2: Glow rings after logo appears
        Handler(Looper.getMainLooper()).postDelayed({
            val glow1Alpha = ObjectAnimator.ofFloat(binding.glowCircle1, "alpha", 0f, 0.3f)
            glow1Alpha.duration = 600
            glow1Alpha.start()

            // Pulsing animation for glow
            startGlowPulse()
        }, 400)

        Handler(Looper.getMainLooper()).postDelayed({
            val glow2Alpha = ObjectAnimator.ofFloat(binding.glowCircle2, "alpha", 0f, 0.15f)
            glow2Alpha.duration = 600
            glow2Alpha.start()
        }, 600)

        // Step 3: App name slides up
        Handler(Looper.getMainLooper()).postDelayed({
            val nameAlpha = ObjectAnimator.ofFloat(binding.appNameText, "alpha", 0f, 1f)
            val nameTransY = ObjectAnimator.ofFloat(binding.appNameText, "translationY", 40f, 0f)
            nameAlpha.duration = 500
            nameTransY.duration = 500
            nameTransY.interpolator = AccelerateDecelerateInterpolator()
            val nameSet = AnimatorSet()
            nameSet.playTogether(nameAlpha, nameTransY)
            nameSet.start()
        }, 600)

        // Step 4: Subtitle slides up
        Handler(Looper.getMainLooper()).postDelayed({
            val subAlpha = ObjectAnimator.ofFloat(binding.subtitleText, "alpha", 0f, 1f)
            val subTransY = ObjectAnimator.ofFloat(binding.subtitleText, "translationY", 30f, 0f)
            subAlpha.duration = 500
            subTransY.duration = 500
            subTransY.interpolator = AccelerateDecelerateInterpolator()
            val subSet = AnimatorSet()
            subSet.playTogether(subAlpha, subTransY)
            subSet.start()
        }, 800)

        // Step 5: Loading dots appear
        Handler(Looper.getMainLooper()).postDelayed({
            val dotsAlpha = ObjectAnimator.ofFloat(binding.loadingDots, "alpha", 0f, 1f)
            dotsAlpha.duration = 400
            dotsAlpha.start()
            startLoadingAnimation()
        }, 1100)

        // Step 6: Version text
        Handler(Looper.getMainLooper()).postDelayed({
            val verAlpha = ObjectAnimator.ofFloat(binding.versionText, "alpha", 0f, 0.5f)
            verAlpha.duration = 400
            verAlpha.start()
        }, 1300)

        // Launch MainActivity after splash duration
        Handler(Looper.getMainLooper()).postDelayed({
            launchMainActivity()
        }, SPLASH_DURATION)
    }

    private fun startGlowPulse() {
        val pulseScale1X = ObjectAnimator.ofFloat(binding.glowCircle1, "scaleX", 1f, 1.15f, 1f)
        val pulseScale1Y = ObjectAnimator.ofFloat(binding.glowCircle1, "scaleY", 1f, 1.15f, 1f)
        pulseScale1X.duration = 1500
        pulseScale1Y.duration = 1500
        pulseScale1X.repeatCount = ObjectAnimator.INFINITE
        pulseScale1Y.repeatCount = ObjectAnimator.INFINITE
        val pulse1 = AnimatorSet()
        pulse1.playTogether(pulseScale1X, pulseScale1Y)
        pulse1.start()
    }

    private fun startLoadingAnimation() {
        // Animate the three dots sequentially
        val dot1 = binding.dot1
        val dot2 = binding.dot2
        val dot3 = binding.dot3

        fun animateDot(view: View, delay: Long) {
            val up = ObjectAnimator.ofFloat(view, "translationY", 0f, -12f)
            val down = ObjectAnimator.ofFloat(view, "translationY", -12f, 0f)
            up.duration = 300
            down.duration = 300
            val set = AnimatorSet()
            set.playSequentially(up, down)
            set.startDelay = delay
            set.repeatCount = AnimatorSet.INFINITE
            // Use a repeating handler instead
            Handler(Looper.getMainLooper()).postDelayed({
                val repeatUp = ObjectAnimator.ofFloat(view, "translationY", 0f, -12f)
                val repeatDown = ObjectAnimator.ofFloat(view, "translationY", -12f, 0f)
                repeatUp.duration = 300
                repeatDown.duration = 300
                val repeatSet = AnimatorSet()
                repeatSet.playSequentially(repeatUp, repeatDown)
                repeatSet.start()
            }, delay)
        }

        animateDot(dot1, 0)
        animateDot(dot2, 200)
        animateDot(dot3, 400)
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        // Disable back button on splash
    }
}
