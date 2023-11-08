package com.example.tripplanner.ui.authentication.signin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tripplanner.ui.authentication.AuthenticationActivity
import com.example.tripplanner.utils.GlobalHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.*


class SignInViewModel(application: Application) : AndroidViewModel(application) {
    private var globalHelper: GlobalHelper = GlobalHelper(application.applicationContext)

    private var _isLogged = MutableLiveData<Boolean>()
    val isLogged: LiveData<Boolean>
        get() = _isLogged

    private var _errorMessageMail = MutableLiveData<String>()
    val errorMessageMail :LiveData<String>
    get() = _errorMessageMail

    private var _errorMessagePassword = MutableLiveData<String>()
    val errorMessagePassword :LiveData<String>
        get() = _errorMessagePassword
    private val id = UUID.randomUUID().toString()


    fun loginWithMail(
        email: String,
        password: String,
        context: Context,
    ) {
        val activity = context as AuthenticationActivity
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) {
                if (it.isSuccessful) {
                    val user = Firebase.auth.currentUser
                    val userEmail = user?.email.toString()
                    globalHelper.setSharedPreferences("Email", userEmail)
                    globalHelper.setBooleanSharedPreferences("isLogged", true)
                    user?.let {
                        val userDocRefInCollection = db.collection("users").document(user.uid)
                        val userDataInDoc = hashMapOf(
                            "userName" to user.displayName,
                            "userEmail" to user.email
                        )
                        userDocRefInCollection.set(userDataInDoc)
                    }
                    _isLogged.postValue(true)
                } else {
                    val exceptionMessage = it.exception.toString()
                    Log.i("SignInViewModelllll","exceptionMessage is $exceptionMessage")
                    if (exceptionMessage.contains("There is no user record corresponding to this identifier. The user may have been deleted.")){
                        _errorMessageMail.postValue("Invalid Email it Doesn't Exist")
                    }else if (exceptionMessage.contains(" A network error (such as timeout, interrupted connection or unreachable host) has occurred.")){
                        _errorMessageMail.postValue("There is No Internet Connection")
                    }else if (exceptionMessage.contains("The password is invalid or the user does not have a password.")){
                        _errorMessagePassword.postValue("Wrong Password Enter it Again, Please")
                    }
                    _isLogged.postValue(false)
                }
            }
    }

    fun getGoogleClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("648176116060-imuo0nu84acktenqcjnd04pklam4uu4u.apps.googleusercontent.com")
            .requestEmail()
            .build()
        val activity = context as Activity
        return GoogleSignIn.getClient(activity, options)
    }

    fun googleSignInUpdateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            val credentials = GoogleAuthProvider.getCredential(account.idToken, null)
            val auth: FirebaseAuth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            auth.signInWithCredential(credentials).addOnCompleteListener {
                if (it.isSuccessful) {
                    val user = Firebase.auth.currentUser
                    val userEmail = user?.email.toString()
                    globalHelper.setSharedPreferences("Email", userEmail)
                    globalHelper.setBooleanSharedPreferences("isLogged", true)
                    user?.let {
                        val userDocRefInCollection = db.collection("users").document(user.uid)
                        val userDataInDoc = hashMapOf(
                            "userName" to user.displayName,
                            "userEmail" to user.email
                        )
                        userDocRefInCollection.set(userDataInDoc)
                    }
                    _isLogged.postValue(true)
                } else {
                    _isLogged.postValue(false)
                }
            }
        }
    }

    fun validateEmail(email: String): Boolean {
        var isValid = true
        if (email.isEmpty()) {
            _errorMessageMail.postValue("Email is Required")
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessageMail.postValue("Email Address is Invalid")
            isValid = false
        }
        return isValid
    }

    fun validatePassword(password: String) :Boolean{
        var isValid = true
        if (password.isEmpty()) {
            _errorMessagePassword.postValue("Password is Required")
            isValid = false
        } else if (password.length < 6) {
            _errorMessagePassword.postValue("Password is Wrong")
            isValid = false
        }

        return isValid
    }


}