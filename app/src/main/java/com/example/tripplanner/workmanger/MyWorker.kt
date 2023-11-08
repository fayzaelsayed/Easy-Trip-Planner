package com.example.tripplanner.workmanger

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.tripplanner.R
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.ui.addtrip.AddTripFragment
import com.example.tripplanner.ui.alertdialog.AlertDialogActivity
import com.example.tripplanner.utils.GlobalHelper
import com.google.gson.Gson

class MyWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    companion object {
        const val CHANNEL_ID = "channel_id"
        const val NOTIFICATION_ID = 1
        const val WORK_NAME = "my_unique_work"
    }

    override fun doWork(): Result {
        try {
            Log.i("doWorkkkkkk", "doWork: success ")
          //  showNotification()
            val tripString = inputData.getString("tripEntity")
            val gson = Gson()
            val tripEntity = gson.fromJson(tripString, TripEntity::class.java)
            val intent = Intent(applicationContext, AlertDialogActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("alertEntity", tripEntity)
            intent.putExtra("music", true)
            applicationContext.startActivity(intent)
            return Result.success()
        } catch (e: Exception) {
            Log.e("MyWorkerllllllllllll", "Error in doWork: ${e.message}")
            return Result.failure()
        }
    }

    //The following snippet shows how to create a basic intent to open an activity when the user taps the notification:
    @SuppressLint("MissingPermission", "RemoteViewLayout", "SuspiciousIndentation")
    private fun showNotification() {
        val intent = Intent(applicationContext, AddTripFragment::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
//        val pendingIntent: PendingIntent =
//            // Use FLAG_IMMUTABLE for targeting Android 12 (API level 31) and above
//            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            // Set the intent that will fire when the user taps the notification
            //.setContentIntent(pendingIntent)
        //.setAutoCancel(false)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Trip Planner"
            val channelDescriptionText = "trip time \n it's time to travel"
            val channelImportance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, channelName, channelImportance)
//                .apply {
//                description = channelDescriptionText
//            }
            // Register the channel with the system

            notificationManager.createNotificationChannel(channel)
        }
        val customLayout = RemoteViews(applicationContext.packageName, R.layout.alert_dialog)
        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setCustomBigContentView(customLayout)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        //To make the notification appear
       // with(NotificationManagerCompat.from(applicationContext)) {
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
       // }

    }
}