package com.example.tripplanner.ui.authentication.forgetpassword

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tripplanner.ui.authentication.AuthenticationActivity
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordViewModel : ViewModel() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var _isSent = MutableLiveData<Boolean>()
    val isSent: LiveData<Boolean>
        get() = _isSent

    private var _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    fun forgetPassword(resetEmail: String, context: Context) {
        val activity = context as AuthenticationActivity
        auth.sendPasswordResetEmail(resetEmail).addOnCompleteListener(activity) {
            if (it.isSuccessful) {
                _isSent.postValue(true)
            } else {
                val exceptionMessage = it.exception.toString()
                Log.i("ForgetPasswordViewModel","exceptionMessage is $exceptionMessage")
                if (exceptionMessage.contains("There is no user record corresponding to this identifier. The user may have been deleted.")){
                    _errorMessage.postValue("Invalid Email it Doesn't Exist")
                }else if (exceptionMessage.contains(" A network error (such as timeout, interrupted connection or unreachable host) has occurred.")){
                    _errorMessage.postValue("There is No Internet Connection")
                }
                _isSent.postValue(false)
            }
        }
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

}