package com.example.tripplanner.ui.alertdialog

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplanner.R
import com.example.tripplanner.database.TripDatabase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlertDialogViewModel(application: Application) :
    AndroidViewModel(application) {
    private val database = TripDatabase.getInstance(application)

    fun updateStatus(key: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataMap = hashMapOf<String, Any>(
                    "tripStatus" to status
                )

                val db = Firebase.firestore
                val user = Firebase.auth.currentUser
                val subCollectionRef =
                    user?.let { db.collection("users").document(user.uid).collection("trips") }
                subCollectionRef?.document(key)?.update(dataMap)
                database.tripDatabaseDao.updateTripStatus(key, status)
            } catch (e: Exception) {
                Log.e("UpcomingTripsViewModel", e.message.toString())
            }
        }
    }

    fun updateDate(key: String, date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataMap = hashMapOf<String, Any>(
                    "date" to date
                )

                val db = Firebase.firestore
                val user = Firebase.auth.currentUser
                val subCollectionRef =
                    user?.let { db.collection("users").document(user.uid).collection("trips") }
                subCollectionRef?.document(key)?.update(dataMap)
                database.tripDatabaseDao.updateTripDate(key, date)
            } catch (e: Exception) {
                Log.e("UpcomingTripsViewModel", e.message.toString())
            }
        }
    }



    fun playMusic(context: Context) {
        var mp = MediaPlayer()
        mp.setDataSource(
            context,
            Uri.parse("android.resource://" + context.packageName + "/" + R.raw.music)
        )
        mp.prepare()
        mp.start()
    }

    fun updateStartTime(key: String, start: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataMap = hashMapOf<String, Any>(
                    "startTime" to start
                )
                val db = Firebase.firestore
                val user = Firebase.auth.currentUser
                val subCollectionRef =
                    user?.let { db.collection("users").document(user.uid).collection("trips") }
                subCollectionRef?.document(key)?.update(dataMap)
                database.tripDatabaseDao.updateStartTime(key, start)
            } catch (e: Exception) {
                Log.e("UpcomingTripsViewModel", e.message.toString())
            }
        }
    }

}