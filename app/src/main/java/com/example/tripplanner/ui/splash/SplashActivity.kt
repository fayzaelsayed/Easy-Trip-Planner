package com.example.tripplanner.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.tripplanner.ui.MainActivity
import com.example.tripplanner.R
import com.example.tripplanner.ui.authentication.AuthenticationActivity
import com.example.tripplanner.databinding.ActivitySplashBinding
import com.example.tripplanner.ui.onboarding.OnBoardingActivity
import com.example.tripplanner.utils.GlobalHelper
import com.google.firebase.FirebaseApp


class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    lateinit var globalHelper: GlobalHelper

    private lateinit var topAnimation: Animation
    private lateinit var bottomAnimation: Animation


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN
//        )

        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        globalHelper = GlobalHelper(this)
        val actionBar = supportActionBar
        actionBar?.hide()


        topAnimation = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)

        binding.ivSplash.animation = topAnimation
        binding.tvFirst.animation = bottomAnimation
        binding.tvSecond.animation = bottomAnimation

        Handler().postDelayed({
            val doneOnBoarding = globalHelper.getBooleanSharedPreferences("onBoard", false)
            val doneAuth = globalHelper.getBooleanSharedPreferences("isLogged", false)
            if (doneOnBoarding) {
                if (doneAuth) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, AuthenticationActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                val intent = Intent(this, OnBoardingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 3000)
    }

}