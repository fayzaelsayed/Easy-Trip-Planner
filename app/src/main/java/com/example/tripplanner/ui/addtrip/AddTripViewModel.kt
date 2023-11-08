package com.example.tripplanner.ui.addtrip

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.tripplanner.BuildConfig
import com.example.tripplanner.database.TripDatabase.Companion.getInstance
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.workmanger.MyWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.TextHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

class AddTripViewModel(application: Application) :
    AndroidViewModel(application) {
    private val database = getInstance(application)
    private val db = FirebaseFirestore.getInstance()
    private val user = Firebase.auth.currentUser
    //private val tripRepository = TripRepository(database)


    private var _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage


    private var _progressBarIsVisible = MutableLiveData<Boolean>(false)
    val progressBarIsVisible: LiveData<Boolean>
        get() = _progressBarIsVisible


    private var _distanceValue = MutableLiveData<String>()
    val distanceValue: LiveData<String>
        get() = _distanceValue

    private var _durationValue = MutableLiveData<String>()
    val durationValue: LiveData<String>
        get() = _durationValue

    private var _requestSuccess = MutableLiveData<Boolean>()
    val requestSuccess: LiveData<Boolean>
        get() = _requestSuccess

    private var workRequest: WorkRequest? = null

    fun requestingForDistanceAndDuration(startPointLatLng: String, endPointLatLng: String) {
        _progressBarIsVisible.postValue(true)
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=$startPointLatLng&destination=$endPointLatLng&key=${BuildConfig.api_key}"
        val asyncHttpClient = AsyncHttpClient()
        asyncHttpClient.get(url, object : TextHttpResponseHandler() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>?,
                responseString: String?
            ) {
                val jsonObject = JSONObject(responseString!!)
                val route = jsonObject.getJSONArray("routes")
                val elementOne = route.getJSONObject(0)
                val legs = elementOne.getJSONArray("legs")
                val elementNumOne = legs.getJSONObject(0)
                val distance = elementNumOne.getJSONObject("distance")
                // val distanceValue = distance.getString("text")
                _distanceValue.postValue(distance.getString("text"))
                val duration = elementNumOne.getJSONObject("duration")
                _durationValue.postValue(duration.getString("text"))

                _requestSuccess.postValue(true)
                _progressBarIsVisible.postValue(false)
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseString: String?,
                throwable: Throwable?
            ) {
                _requestSuccess.postValue(false)
                _progressBarIsVisible.postValue(false)
                // _distanceValue.postValue("not avaialbe")

            }

        })
    }

    fun myWorkManagerRequest(
        hMonth: Int,
        hDay: Int,
        hHour: Int,
        hMinute: Int,
        context: Context,
        trip: TripEntity,
        numberOfWork: Long
    ) {

        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        // Set the desired month, day, and hour
        dueDate.set(Calendar.MONTH, hMonth)
        dueDate.set(Calendar.DAY_OF_MONTH, hDay)
        dueDate.set(Calendar.HOUR_OF_DAY, hHour)
        dueDate.set(Calendar.MINUTE, hMinute)
        dueDate.set(Calendar.SECOND, 0)


        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        Log.i("Gggggggggggg", "myOneTimeWork: $timeDiff")
        val gson = Gson()
        val tripString = gson.toJson(trip)
        val data = Data.Builder().putString("tripEntity", tripString).build()

        if (numberOfWork == -1L) {
            // Schedule the work
            workRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(trip.tripId)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest as OneTimeWorkRequest)
        } else {
            workRequest = PeriodicWorkRequest.Builder(
                MyWorker::class.java,
                numberOfWork,
                TimeUnit.DAYS
            ).setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .addTag(trip.tripId)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest as PeriodicWorkRequest)
        }
    }

//    fun getWorkInfoLiveData(context: Context): LiveData<List<WorkInfo>> {
//        // Define a query for the WorkManager
//        val query = WorkQuery.Builder
//            .fromUniqueWorkNames(listOf(MyWorker.WORK_NAME))
//            .addStates(
//                listOf(
//                    WorkInfo.State.ENQUEUED,
//                    WorkInfo.State.RUNNING,
//                    WorkInfo.State.SUCCEEDED
//                )
//            )
//            .build()
//
//        // Use WorkManager's getWorkInfosLiveData method to observe work
//        return WorkManager.getInstance(context).getWorkInfosLiveData(query)
//    }

    fun insertTripToDatabaseAndFirebase(newTrip: TripEntity) {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                database.tripDatabaseDao.insertTrip(newTrip)
                val subCollectionRef =
                    user?.let {
                        db.collection("users").document(it.uid).collection("trips")
                    }
                subCollectionRef?.document(newTrip.tripId)?.set(newTrip)
            }
        } catch (e: Exception) {
            Log.e("AddTripViewModel", e.message.toString())
        }
    }


    fun updateTrip(trip: TripEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.tripDatabaseDao.updateTrip(trip)
            } catch (e: Exception) {
                Log.e("Add", e.message.toString())
            }
        }
    }


    fun validTripName(tripName: String): Boolean {
        var isValid = true
        if (tripName.trim().isEmpty()) {
            isValid = false
            _errorMessage.postValue("Trip Name Is Required")
        }
        return isValid
    }


    fun validStartPoint(startPoint: String): Boolean {
        var isValid = true
        if (startPoint.trim().isEmpty()) {
            isValid = false
            _errorMessage.postValue("Start Point Is Required")
        }
        return isValid
    }

    fun validEndPoint(endPoint: String): Boolean {
        var isValid = true
        if (endPoint.trim().isEmpty()) {
            isValid = false
            _errorMessage.postValue("End Point Is Required")
        }
        return isValid
    }

//    suspend fun getAllTripsFromDatabase() {
//        return withContext(Dispatchers.IO) {
//            try {
//                database.tripDatabaseDao.getAllTrips()
//            } catch (e: Exception) {
//                Log.e("AddTripViewModel", e.message.toString())
//            }
//        }
//    }


//    fun addTrip(newTrip: TripEntity) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                tripRepository.insertTripToDatabase(newTrip)
//            } catch (e: Exception) {
//                Log.e("AddTripViewModel", e.message.toString())
//            }
//        }
//    }
//    fun getAllTrips() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                tripRepository.getAllTripsFromDatabase()
//            } catch (e: Exception) {
//                Log.e("AddTripViewModel", e.message.toString())
//            }
//        }
//    }


}