package com.example.tripplanner.ui.pasttrips

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.tripplanner.database.TripDatabase
import com.example.tripplanner.database.TripEntity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PastTripsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TripDatabase.getInstance(application)

    fun getUserTrips(email: String): LiveData<List<TripEntity>> {
        return database.tripDatabaseDao.getAllHistoryUserTrips(email)
    }

    fun getFinishedTrips(email: String): LiveData<List<TripEntity>> {
        return database.tripDatabaseDao.getAllFinishedTrips(email)
    }
    fun deleteTrip(tripEntity: TripEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.tripDatabaseDao.deleteTripById(tripEntity)
                val db = Firebase.firestore
                val user = Firebase.auth.currentUser
                val subCollectionRef =
                    user?.let {
                        db.collection("users").document(user.uid).collection("trips")
                    }
                subCollectionRef?.document(tripEntity.tripId)?.delete()
            } catch (e: Exception) {
                Log.e("UpcomingTripsViewModel", e.message.toString())
            }
        }
    }
}