package com.example.tripplanner.ui.authentication.signin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.example.tripplanner.ui.MainActivity
import com.example.tripplanner.R
import com.example.tripplanner.databinding.FragmentSignInBinding
import com.example.tripplanner.utils.GlobalHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient


class SignInFragment : Fragment(), View.OnFocusChangeListener {
    private lateinit var binding: FragmentSignInBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var globalHelper: GlobalHelper
    private lateinit var viewModel: SignInViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[SignInViewModel::class.java]
        globalHelper = GlobalHelper(requireActivity())

        viewModel.isLogged.observe(viewLifecycleOwner) { isSuccess ->
            isSuccess?.let {
                observeIsLogged(isSuccess)
            }
        }
        viewModel.errorMessageMail.observe(viewLifecycleOwner) { message ->
            message?.let {
                binding.tvEmailSignin.apply {
                    isErrorEnabled = true
                    error = message
                }
            }
        }

        viewModel.errorMessagePassword.observe(viewLifecycleOwner) { message ->
            message?.let {
                binding.tvPasswordSignin.apply {
                    isErrorEnabled = true
                    error = message
                }
            }
        }

        binding.apply {
            tvSignUp.setOnClickListener { view: View ->
                view.findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
            }
            tvForgotPassword.setOnClickListener { view: View ->
                view.findNavController()
                    .navigate(R.id.action_signInFragment_to_forgetPasswordFragment)
            }
            btnLoginEmail.setOnClickListener {
                loginWithMailAndPassword()
            }
            btnGoogle.setOnClickListener {
                googleSignIn()
            }
            edtEmailSignin.onFocusChangeListener = this@SignInFragment
            edtPasswordSignin.onFocusChangeListener = this@SignInFragment
        }

        googleSignInClient = viewModel.getGoogleClient(requireActivity())
        googleSignInClient.signOut()

        return binding.root
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        when (view.id) {
            R.id.edt_email_signin -> {
                if (hasFocus) {
                    if (binding.tvEmailSignin.isErrorEnabled) {
                        binding.tvEmailSignin.isErrorEnabled = false
                    }
                } else {
                    val email = binding.edtEmailSignin.text.toString()
                    viewModel.validateEmail(email)
                }
            }

            R.id.edt_password_signin -> {
                if (hasFocus) {
                    if (binding.tvPasswordSignin.isErrorEnabled) {
                        binding.tvPasswordSignin.isErrorEnabled = false
                    }
                } else {
                    val password = binding.edtPasswordSignin.text.toString()
                    viewModel.validatePassword(password)

                }
            }
        }
    }


    private fun loginWithMailAndPassword() {
        val email = binding.edtEmailSignin.text.toString()
        val password = binding.edtPasswordSignin.text.toString()
        if (viewModel.validateEmail(email) && viewModel.validatePassword(password)) {
            viewModel.loginWithMail(email, password, requireActivity())
            binding.tvSignUp.isEnabled = false
            binding.tvForgotPassword.isEnabled = false
            binding.btnLoginEmail.isEnabled = false
            binding.btnGoogle.isEnabled = false
        }
    }

    private fun observeIsLogged(isSuccess: Boolean) {
        if (isSuccess) {
            Toast.makeText(
                requireContext(),
                "Successfully Logged in",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                this@SignInFragment.requireContext(),
                MainActivity::class.java
            )
            startActivity(intent)
            requireActivity().finishAffinity()
        } else {
            Toast.makeText(
                requireContext(),
                "Wrong Details",
                Toast.LENGTH_LONG
            ).show()
            binding.tvSignUp.isEnabled = true
            binding.tvForgotPassword.isEnabled = true
            binding.btnLoginEmail.isEnabled = true
            binding.btnGoogle.isEnabled = false
        }

    }

    private fun googleSignIn() {
        val intent = googleSignInClient.signInIntent
        launcher.launch(intent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK){
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

                if (task.isSuccessful) {
                    val account: GoogleSignInAccount? = task.result
                    viewModel.googleSignInUpdateUI(account)
                } else {
                    Toast.makeText(requireContext(), "Error Occurred", Toast.LENGTH_LONG).show()
                }
            }
        }
}