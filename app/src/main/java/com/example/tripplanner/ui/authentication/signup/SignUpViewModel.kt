package com.example.tripplanner.ui.authentication.signup

import android.app.Application
import android.content.Context
import android.util.Log
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

    private val _isLogged = MutableLiveData<Boolean>()
    val isLogged: LiveData<Boolean>
        get() = _isLogged

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _errorMessageFirebase = MutableLiveData<String>()
    val errorMessageFirebase: LiveData<String>
        get() = _errorMessageFirebase



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
                    val exceptionMessage = it.exception.toString()
                    Log.i("eeeeeeee", "mailSignup:$exceptionMessage ")
                    _isLogged.postValue(false)
                    if (exceptionMessage.contains("The email address is already in use by another account.")){
                        _errorMessageFirebase.postValue("The email address is already in use.")
                    } else if (exceptionMessage.contains("A network error (such as timeout, interrupted connection or unreachable host) has occurred.")){
                        _errorMessageFirebase.postValue("Network error, please check your network connection")
                    }else if(exceptionMessage.contains("The email address is badly formatted.")){
                        _errorMessageFirebase.postValue("The email address is badly formatted.")
                    }else{
                        _errorMessageFirebase.postValue("SignUp Failed")
                    }
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