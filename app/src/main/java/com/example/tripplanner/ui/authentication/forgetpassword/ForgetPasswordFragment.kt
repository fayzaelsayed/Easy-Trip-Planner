package com.example.tripplanner.ui.authentication.forgetpassword

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.example.tripplanner.R
import com.example.tripplanner.databinding.FragmentForgetPasswordBinding

class ForgetPasswordFragment : Fragment(), View.OnFocusChangeListener {
    lateinit var binding: FragmentForgetPasswordBinding
    private lateinit var viewModel: ForgetPasswordViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            FragmentForgetPasswordBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ForgetPasswordViewModel::class.java]

        binding.apply {
            edtEmailReset.onFocusChangeListener = this@ForgetPasswordFragment

            tvBackSignin.setOnClickListener { view: View ->
                view.findNavController()
                    .navigate(R.id.action_forgetPasswordFragment_to_signInFragment)
            }
                btnSend.setOnClickListener {
                    forgetPassword()
                }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.tvEmailReset.apply {
                    isErrorEnabled = true
                    error = it
                }
            }
        }

        viewModel.isSent.observe(viewLifecycleOwner) { isSuccess ->
            isSuccess?.let {
                observeIsSent(isSuccess)
            }
        }


        return binding.root
    }


    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        if (view != null) {
            when (view.id) {
                R.id.edt_email_signin -> {
                    if (hasFocus) {
                        if (binding.tvEmailReset.isErrorEnabled) {
                            binding.tvEmailReset.isErrorEnabled = false
                        }
                    } else {
                        val email = binding.edtEmailReset.text.toString()
                        viewModel.validateEmail(email)
                    }
                }
            }

        }
    }

    private fun forgetPassword() {
        Log.i("ssssss", "forgetPassword: beforeIf ")
        val email = binding.edtEmailReset.text.toString()
        if (viewModel.validateEmail(email)) {
            Log.i("ssssss", "forgetPassword: insideIf ")
            val resetEmail = binding.edtEmailReset.text.toString()
            viewModel.forgetPassword(resetEmail, requireActivity())
            binding.tvBackSignin.isEnabled = false
            binding.btnSend.isEnabled = false
        }
    }

    private fun observeIsSent(isSuccess: Boolean) {
        if (isSuccess) {
            Toast.makeText(
                requireContext(),
                "Please Check Your Email",
                Toast.LENGTH_LONG
            ).show()
            binding.tvBackSignin.isEnabled = true
            binding.btnSend.isEnabled = true
        } else {
            Toast.makeText(
                requireContext(),
                "Error Occurred",
                Toast.LENGTH_LONG
            ).show()
            binding.tvBackSignin.isEnabled = true
            binding.btnSend.isEnabled = true
        }
    }
}