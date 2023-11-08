package com.example.tripplanner.ui.authentication.signup

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.tripplanner.R
import com.example.tripplanner.databinding.FragmentSignUpBinding


class SignUpFragment : Fragment(), View.OnFocusChangeListener {
    private lateinit var binding: FragmentSignUpBinding
    private lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        binding.tvSignin.setOnClickListener { view: View ->
            view.findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }
        viewModel = ViewModelProvider(this)[SignUpViewModel::class.java]

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                observeErrorMessage(errorMessage)
            }
        }
        viewModel.errorMessageFirebase.observe(viewLifecycleOwner){
            if (!it.isNullOrEmpty()){
                Toast.makeText(requireContext(), it,Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLogged.observe(viewLifecycleOwner) { isSuccess ->
            isSuccess.let {
                observeIsLogged(isSuccess)
            }
        }

        binding.apply {
            edtName.onFocusChangeListener = this@SignUpFragment
            edtEmailSignup.onFocusChangeListener = this@SignUpFragment
            edtPasswordSignup.onFocusChangeListener = this@SignUpFragment
            edtConfirmPasswordSignup.onFocusChangeListener = this@SignUpFragment
            btnSignup.setOnClickListener {
                signUpWithMail()
            }
        }
        return binding.root
    }

    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        if (view != null) {
            when (view.id) {
                R.id.edt_name -> {
                    if (hasFocus) {
                        if (binding.tvName.isErrorEnabled) {
                            binding.tvName.isErrorEnabled = false
                        }
                    } else {
                        val name = binding.edtName.text.toString()
                        viewModel.validateUserName(name)
                    }
                }
                R.id.edt_email_signup -> {
                    if (hasFocus) {
                        if (binding.tvEmailSignup.isErrorEnabled) {
                            binding.tvEmailSignup.isErrorEnabled = false
                        }
                    } else {
                        val email = binding.edtEmailSignup.text.toString()
                        viewModel.validateEmail(email)
                    }
                }
                R.id.edt_password_signup -> {
                    if (hasFocus) {
                        if (binding.tvPasswordSignup.isErrorEnabled) {
                            binding.tvPasswordSignup.isErrorEnabled = false
                        }
                    } else {
                        val password = binding.edtPasswordSignup.text.toString()
                        if (viewModel.validatePassword(password)) {
                            if (binding.tvPasswordSignup.isErrorEnabled) {
                                binding.tvPasswordSignup.isErrorEnabled = false
                            }
                            binding.tvPasswordSignup.apply {
                                setStartIconDrawable(R.drawable.check_circle_icon)
                                setStartIconTintList(ColorStateList.valueOf(Color.GREEN))
                            }
                        }
                    }
                }
                R.id.edt_confirm_password_signup -> {
                    if (hasFocus) {
                        if (binding.tvConfirmPasswordSignup.isErrorEnabled) {
                            binding.tvConfirmPasswordSignup.isErrorEnabled = false
                        }
                    } else {
                        val confirmPassword = binding.edtConfirmPasswordSignup.text.toString()
                        val password = binding.edtPasswordSignup.text.toString()
                        if (viewModel.validateConfirmPassword(confirmPassword) && viewModel.validatePasswordAndConfirmPasswordEquality(
                                password,
                                confirmPassword
                            )
                        ) {
                            if (binding.tvPasswordSignup.isErrorEnabled) {
                                binding.tvPasswordSignup.isErrorEnabled = false
                            }
                            binding.tvConfirmPasswordSignup.apply {
                                setStartIconDrawable(R.drawable.check_circle_icon)
                                setStartIconTintList(ColorStateList.valueOf(Color.GREEN))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun signUpWithMail() {
        val email = binding.edtEmailSignup.text.toString()
        val password = binding.edtPasswordSignup.text.toString()
        val name = binding.edtName.text.toString()
        val confirmPassword = binding.edtConfirmPasswordSignup.text.toString()
        if (viewModel.validateUserName(name) && viewModel.validateEmail(email) && viewModel.validatePassword(
                password
            ) && viewModel.validateConfirmPassword(confirmPassword) && viewModel.validatePasswordAndConfirmPasswordEquality(
                password,
                confirmPassword
            )
        ) {
            viewModel.mailSignup(email, password, name, requireActivity())
            binding.tvSignin.isEnabled = false
            binding.btnSignup.isEnabled = false
            binding.btnGoogle.isEnabled = false
        }
    }

    private fun observeIsLogged(isSuccess: Boolean) {
        if (isSuccess) {
            Toast.makeText(
                requireContext(),
                "User Has Been Added Successfully",
                Toast.LENGTH_LONG
            ).show()
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        } else {
            Toast.makeText(
                requireContext(),
                "Failed to Add a User",
                Toast.LENGTH_LONG
            ).show()

            binding.tvSignin.isEnabled = true
            binding.btnSignup.isEnabled = true
            binding.btnGoogle.isEnabled = true
        }
    }

    private fun observeErrorMessage(errorMessage: String) {
        when (errorMessage) {
            "User Name is Required" -> binding.tvName.apply {
                isErrorEnabled = true
                error = errorMessage
            }
            "Email is Required" -> binding.tvEmailSignup.apply {
                isErrorEnabled = true
                error = errorMessage
            }
            "Email Address is Invalid" -> binding.tvEmailSignup.apply {
                isErrorEnabled = true
                error = errorMessage
            }
            "Password is Required" -> binding.tvPasswordSignup.apply {
                isErrorEnabled = true
                error = errorMessage
            }
            "Password Must Be at Least 6 Characters long" -> binding.tvPasswordSignup.apply {
                isErrorEnabled = true
                error = errorMessage
            }
            "Confirm Password is Required" -> binding.tvConfirmPasswordSignup.apply {
                isErrorEnabled = true
                error = errorMessage
            }
            "Password and its Confirm Must Be at Least 6 Characters long" -> binding.tvConfirmPasswordSignup.apply {
                isErrorEnabled = true
                error = errorMessage
            }
            "Confirm Password Does Not Match with Password" ->
                binding.tvConfirmPasswordSignup.apply {
                    isErrorEnabled = true
                    error = errorMessage
                }

        }
    }
}