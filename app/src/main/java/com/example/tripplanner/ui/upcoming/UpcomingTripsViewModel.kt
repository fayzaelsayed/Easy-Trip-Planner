package com.example.tripplanner.ui.upcoming

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.tripplanner.database.TripDatabase
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.workmanger.MyWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
class UpcomingTripsViewModel(application: Application) :
    AndroidViewModel(application) {
    private val database = TripDatabase.getInstance(application)
    private var _showProgress = MutableLiveData(true)
    val showProgress: LiveData<Boolean>
        get() = _showProgress


    fun showDialog(tripEntity: TripEntity, context: Context) {
        val list = tripEntity.note.split(",*herewecansplitit")
        val selectedItems = ArrayList<Int>()
        val checkedItems = BooleanArray(list.size)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("The notes of the trip").setMultiChoiceItems(
            list.toTypedArray(),
            checkedItems,
            DialogInterface.OnMultiChoiceClickListener { dialog, which, isChecked ->
                if (isChecked) {
                    selectedItems.add(which)
                } else {
                    selectedItems.remove(which)
                }
                checkedItems[which] = isChecked
            }).setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
            //  Toast.makeText(context, "$selectedItems", Toast.LENGTH_LONG).show()
            for (item in checkedItems) {
                val i = item

            }
        }).setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
        })

        //val checkedItems = BooleanArray(list.size) { selectedItems.contains(it) }
//        for (itemIndex in selectedItems) {
//            checkedItems[itemIndex] = true
//        }
        builder.setMultiChoiceItems(list.toTypedArray(), checkedItems) { _, which, isChecked ->
            if (isChecked) {
                selectedItems.add(which)
            } else {
                selectedItems.remove(which)
            }
        }

        val dialog = builder.create()
//        dialog.setOnShowListener {
//            for (i in selectedItems) {
//                dialog.listView.setItemChecked(i, true)
//            }
//        }
        dialog.show()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getTripsFromFireStore() {
        //  val database = TripDatabase.getInstance(application)
        val db = FirebaseFirestore.getInstance()
        val user = Firebase.auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).collection("trips").get()
                .addOnSuccessListener {
                    it?.let {
                        for (document in it) {
                            val entity = TripEntity(
                                tripId = document.getString("tripId") ?: "",
                                tripName = document.getString("tripName") ?: "",
                                startPoint = document.getString("startPoint") ?: "",
                                endPoint = document.getString("endPoint") ?: "",
                                note = document.getString("note") ?: "",
                                date = document.getString("date") ?: "",
                                time = document.getString("time") ?: "",
                                email = user.email ?: "",
                                tripStatus = document.getString("tripStatus") ?: "",
                                startPointLatLng = document.getString("startPointLatLng") ?: "",
                                endPointLatLng = document.getString("endPointLatLng") ?: "",
                                insertionTime = document.getLong("insertionTime") ?: 0L,
                                tripDistance = document.getString("tripDistance") ?: "",
                                tripDuration = document.getString("tripDuration") ?: "",
                                startTime = document.getLong("startTime") ?: 0L,
                                workRequest = document.getLong("workRequest") ?: 0L,
                                source = "FIREBASE",
                                tripType = document.getString("tripType") ?: "",
                                roundedId = document.getString("roundedId") ?: ""
                            )
                            viewModelScope.launch(Dispatchers.IO) {
                                database.tripDatabaseDao.insertTrip(entity)
                            }
                        }
                        _showProgress.postValue(false)
                        Log.i("hhhhhhhhhhhhhh", "getTripsFromFirestore: insertedTrips")
                    }
                }.addOnFailureListener {
                    _showProgress.postValue(false)
                }
        }
    }

    fun getUserTrips(email: String): LiveData<List<TripEntity>> {
        //getTripsFromFirestore()
        Log.i("hhhhhhhhhhhhhh", "getUserTrips: after adding trips")
        return _showProgress.switchMap {
            if (!it) {
                database.tripDatabaseDao.getAllUpcomingUserTrips(email)
            } else {
                null
            }
        }
    }

    fun myWorkManagerRequest(
        context: Context,
        trip: TripEntity,
        numberOfWork: Long
    ) {
        val combinedString =
            StringBuilder().append(trip.date).append(" ").append(trip.time).toString()
        val currentTime = System.currentTimeMillis()
        // Define the date format for your input string
        val inputDateFormat = SimpleDateFormat("dd.MM yyyy HH:mm")
        // Parse the input date string
        val inputDate = inputDateFormat.parse(combinedString)
        // Calculate the time difference
        val timeDifferenceInMillis = inputDate.time - currentTime

        val gson = Gson()
        val tripString = gson.toJson(trip)
        val data = Data.Builder().putString("tripEntity", tripString).build()

        if (numberOfWork == -1L) {
            // Schedule the work
            val workRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
                .setInitialDelay(timeDifferenceInMillis, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(trip.tripId)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest as OneTimeWorkRequest)
        } else {
            repeatTheRequest(trip, numberOfWork, timeDifferenceInMillis, data, context)
        }
    }


    private fun repeatTheRequest(
        trip: TripEntity,
        numberOfWork: Long,
        timeDiff: Long,
        data: Data,
        context: Context
    ) {
        val workRequest = PeriodicWorkRequest.Builder(
            MyWorker::class.java,
            numberOfWork,
            TimeUnit.DAYS
        ).setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag(trip.tripId)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest as PeriodicWorkRequest)
    }

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