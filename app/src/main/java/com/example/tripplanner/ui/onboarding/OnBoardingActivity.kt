package com.example.tripplanner.ui.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.tripplanner.R
import com.example.tripplanner.databinding.ActivityOnBoardingBinding


class OnBoardingActivity : AppCompatActivity() {
    lateinit var binding: ActivityOnBoardingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_on_boarding)

        val fragmentList = arrayListOf(FirstFragment(), SecondFragment(), ThirdFragment())
        val adapter = OnBoardingViewPagerAdapter(fragmentList, supportFragmentManager, lifecycle)
        binding.apply {
            slideViewPager.adapter = adapter
            dotsIndicator.attachTo(binding.slideViewPager)
        }
    }
}