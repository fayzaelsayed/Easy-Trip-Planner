package com.example.tripplanner.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.tripplanner.utils.GlobalHelper
import com.example.tripplanner.ui.MainActivity
import com.example.tripplanner.R
import com.example.tripplanner.ui.authentication.AuthenticationActivity
import com.example.tripplanner.databinding.FragmentThirdBinding


class ThirdFragment : Fragment() {
    lateinit var binding: FragmentThirdBinding
    lateinit var globalHelper: GlobalHelper
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_third, container, false)
        globalHelper = GlobalHelper(requireActivity())
        binding.btnNextThird.setOnClickListener {
            stayLoggedInAndShowOnBoardingOnce()
        }
        return binding.root
    }

    private fun stayLoggedInAndShowOnBoardingOnce(){
        val isLogIn = globalHelper.getBooleanSharedPreferences("isLogged", false)
        if (isLogIn!!) {
            val intent = Intent(
                requireContext(),
                MainActivity::class.java
            )
            startActivity(intent)
            requireActivity().finishAffinity()
        }else{
            val intent = Intent(this@ThirdFragment.requireContext(), AuthenticationActivity::class.java)
            startActivity(intent)
            requireActivity().finishAffinity()
        }
        globalHelper.setBooleanSharedPreferences("onBoard",true)
    }
}