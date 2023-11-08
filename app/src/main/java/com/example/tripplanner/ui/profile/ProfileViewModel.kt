package com.example.tripplanner.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tripplanner.database.TripDatabase
import com.example.tripplanner.database.TripEntity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase

class ProfileViewModel(application: Application) :
    AndroidViewModel(application) {

    private var _countUpcoming = MutableLiveData<Int>()
    val countUpcoming: LiveData<Int>
        get() = _countUpcoming

    private var _countFinished = MutableLiveData<Int>()
    val countFinished: LiveData<Int>
        get() = _countFinished

    private var _countCanceled = MutableLiveData<Int>()
    val countCanceled: LiveData<Int>
        get() = _countCanceled

    val database = TripDatabase.getInstance(application)
    val db = FirebaseFirestore.getInstance()
    val user = Firebase.auth.currentUser
    val collectionReference =
        db.collection("users").document(user!!.uid).collection("trips")


    fun numberOfUpcomingTrips() {
        val upcomingQuery: Query = collectionReference.whereEqualTo("tripStatus", "UPCOMING")
        upcomingQuery.get().addOnSuccessListener {
            _countUpcoming.postValue(it.size())
        }.addOnFailureListener {

        }
    }

    fun numberOfFinishedTrips() {
        val upcomingQuery: Query = collectionReference.whereEqualTo("tripStatus", "FINISHED")
        upcomingQuery.get().addOnSuccessListener {
            _countFinished.postValue(it.size())
        }.addOnFailureListener {

        }
    }

    fun numberOfCanceledTrips() {
        val upcomingQuery: Query = collectionReference.whereEqualTo("tripStatus", "CANCELED")
        upcomingQuery.get().addOnSuccessListener {
            _countCanceled.postValue(it.size())
        }.addOnFailureListener {

        }
    }

    fun getUpcomingTrips(email: String): LiveData<List<TripEntity>> {
        return database.tripDatabaseDao.getAllUpcomingUserTrips(email)


    }

    fun getFinishedTrips(email: String): LiveData<List<TripEntity>> {
        return database.tripDatabaseDao.getAllFinishedTrips(email)
    }

    fun getCanceledTrips(email: String): LiveData<List<TripEntity>> {
        return database.tripDatabaseDao.getAllCanceledTrips(email)
    }

    fun getAllTrips(email: String):  LiveData<List<TripEntity>>{
        return database.tripDatabaseDao.getAllTrips(email)
    }
}