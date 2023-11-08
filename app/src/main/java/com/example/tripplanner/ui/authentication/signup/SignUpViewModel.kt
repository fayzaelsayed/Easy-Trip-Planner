package com.example.tripplanner.ui.authentication.signup

import android.app.Application
import android.content.Context
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tripplanner.ui.authentication.AuthenticationActivity
import com.example.tripplanner.utils.GlobalHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SignUpViewModel(application: Application) : AndroidViewModel(application) {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var globalHelper: GlobalHelper = GlobalHelper(application.applicationContext)

    private var _isLogged = MutableLiveData<Boolean>()
    val isLogged: LiveData<Boolean>
        get() = _isLogged

    private var _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage


    fun mailSignup(email: String, password: String,name: String, context: Context) {
        val activity = context as AuthenticationActivity
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) {
                if (it.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                globalHelper.setSharedPreferences("Name",name)
                            }
                        }
                    _isLogged.postValue(true)
                } else {
                    _isLogged.postValue(false)
                }
            }
    }

    fun validateUserName(userName: String): Boolean {
        var isValid = true
        if (userName.trim().isEmpty()) {
            _errorMessage.postValue("User Name is Required")
            isValid = false
        }

        return isValid
    }

    fun validateEmail(email: String): Boolean {
        var isValid = true
        if (email.isEmpty()) {
            _errorMessage.postValue("Email is Required")
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
           _errorMessage.postValue("Email Address is Invalid")
            isValid = false
        }

        return isValid
    }

    fun validatePassword(password: String): Boolean {
        var isValid = true
        if (password.isEmpty()) {
            _errorMessage.postValue("Password is Required")
            isValid = false
        } else if (password.length < 6) {
            _errorMessage.postValue("Password Must Be at Least 6 Characters long")
            isValid = false
        }
        return isValid
    }

    fun validateConfirmPassword(password:String): Boolean {
        var isValid = true
        if (password.isEmpty()) {
            _errorMessage.postValue("Confirm Password is Required")
            isValid = false
        } else if (password.length < 6) {
           _errorMessage.postValue("Password Must Be at Least 6 Characters long")
            isValid = false
        }

        return isValid
    }

    fun validatePasswordAndConfirmPasswordEquality(password: String, confirmPassword:String): Boolean {
        var isValid = true
        if (password != confirmPassword) {
           _errorMessage.postValue("Confirm Password Does Not Match with Password")
            isValid = false
        }
        return isValid
    }

}