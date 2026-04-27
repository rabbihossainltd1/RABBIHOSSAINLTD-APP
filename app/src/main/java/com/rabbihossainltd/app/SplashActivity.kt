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
    private val splashDuration = 2500L
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        startSplashAnimations()
    }

    private fun startSplashAnimations() {
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

        val logoScaleX = ObjectAnimator.ofFloat(binding.logoContainer, "scaleX", 0.3f, 1.1f, 1.0f)
        val logoScaleY = ObjectAnimator.ofFloat(binding.logoContainer, "scaleY", 0.3f, 1.1f, 1.0f)
        val logoAlpha = ObjectAnimator.ofFloat(binding.logoContainer, "alpha", 0f, 1f)
        logoScaleX.duration = 700
        logoScaleY.duration = 700
        logoAlpha.duration = 500
        logoScaleX.interpolator = OvershootInterpolator(2.0f)
        logoScaleY.interpolator = OvershootInterpolator(2.0f)

        AnimatorSet().apply {
            playTogether(logoScaleX, logoScaleY, logoAlpha)
            start()
        }

        handler.postDelayed({
            ObjectAnimator.ofFloat(binding.glowCircle1, "alpha", 0f, 0.3f).apply {
                duration = 600
                start()
            }
            startGlowPulse()
        }, 400)

        handler.postDelayed({
            ObjectAnimator.ofFloat(binding.glowCircle2, "alpha", 0f, 0.15f).apply {
                duration = 600
                start()
            }
        }, 600)

        handler.postDelayed({
            val nameAlpha = ObjectAnimator.ofFloat(binding.appNameText, "alpha", 0f, 1f)
            val nameTransY = ObjectAnimator.ofFloat(binding.appNameText, "translationY", 40f, 0f)
            nameAlpha.duration = 500
            nameTransY.duration = 500
            nameTransY.interpolator = AccelerateDecelerateInterpolator()
            AnimatorSet().apply {
                playTogether(nameAlpha, nameTransY)
                start()
            }
        }, 600)

        handler.postDelayed({
            val subAlpha = ObjectAnimator.ofFloat(binding.subtitleText, "alpha", 0f, 1f)
            val subTransY = ObjectAnimator.ofFloat(binding.subtitleText, "translationY", 30f, 0f)
            subAlpha.duration = 500
            subTransY.duration = 500
            subTransY.interpolator = AccelerateDecelerateInterpolator()
            AnimatorSet().apply {
                playTogether(subAlpha, subTransY)
                start()
            }
        }, 800)

        handler.postDelayed({
            ObjectAnimator.ofFloat(binding.loadingDots, "alpha", 0f, 1f).apply {
                duration = 400
                start()
            }
            startLoadingAnimation()
        }, 1100)

        handler.postDelayed({
            ObjectAnimator.ofFloat(binding.versionText, "alpha", 0f, 0.5f).apply {
                duration = 400
                start()
            }
        }, 1300)

        handler.postDelayed({ launchMainActivity() }, splashDuration)
    }

    private fun startGlowPulse() {
        val pulseScale1X = ObjectAnimator.ofFloat(binding.glowCircle1, "scaleX", 1f, 1.15f, 1f)
        val pulseScale1Y = ObjectAnimator.ofFloat(binding.glowCircle1, "scaleY", 1f, 1.15f, 1f)
        pulseScale1X.duration = 1500
        pulseScale1Y.duration = 1500
        pulseScale1X.repeatCount = ObjectAnimator.INFINITE
        pulseScale1Y.repeatCount = ObjectAnimator.INFINITE

        AnimatorSet().apply {
            playTogether(pulseScale1X, pulseScale1Y)
            start()
        }
    }

    private fun startLoadingAnimation() {
        fun animateDot(view: View, delay: Long) {
            handler.postDelayed(object : Runnable {
                override fun run() {
                    val up = ObjectAnimator.ofFloat(view, "translationY", 0f, -12f)
                    val down = ObjectAnimator.ofFloat(view, "translationY", -12f, 0f)
                    up.duration = 300
                    down.duration = 300

                    AnimatorSet().apply {
                        playSequentially(up, down)
                        start()
                    }

                    handler.postDelayed(this, 900)
                }
            }, delay)
        }

        animateDot(binding.dot1, 0)
        animateDot(binding.dot2, 200)
        animateDot(binding.dot3, 400)
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Disable back button on splash screen.
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
