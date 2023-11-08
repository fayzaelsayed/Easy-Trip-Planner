package com.example.tripplanner.ui.alertdialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.example.tripplanner.R
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.databinding.ActivityAlertDialogBinding
import com.example.tripplanner.ui.upcoming.FloatingBubbleService
import java.util.*

class AlertDialogActivity : AppCompatActivity() {
    lateinit var binding: ActivityAlertDialogBinding
    private lateinit var viewModel: AlertDialogViewModel
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var alertTripEntity: TripEntity? = null
    private var mp: MediaPlayer? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alertTripEntity = intent.getParcelableExtra("alertEntity")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_alert_dialog)
        val isFromWork = intent.getBooleanExtra("music", true)

        viewModel = ViewModelProvider(this)[AlertDialogViewModel::class.java]

        startFloatingBubbleLauncher()


        if (isFromWork) {
            playMusicAndUpdateDate()
        }
        binding.notificationText.text = "Trip Name: ${alertTripEntity?.tripName}"
        binding.btnBeginTripAlert.setOnClickListener {
            if (alertTripEntity != null) {
                stopMediaPlayer()
                startTrip(alertTripEntity!!)
                WorkManager.getInstance(this.applicationContext).cancelAllWorkByTag(
                    alertTripEntity!!.tripId
                )
            }
            finish()
        }

        binding.btnLaterAlert.setOnClickListener {
            stopMediaPlayer()
            createNotification(alertTripEntity!!)
            finish()
        }

        binding.btnCancelAlert.setOnClickListener {
            stopMediaPlayer()
            if (alertTripEntity?.workRequest == -1L) {
                viewModel.updateStatus(alertTripEntity!!.tripId, "CANCELED")
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1)
            WorkManager.getInstance(this.applicationContext).cancelAllWorkByTag(
                alertTripEntity!!.tripId
            )
            finish()
        }

    }

    private fun playMusicAndUpdateDate() {
        mp = MediaPlayer.create(this, R.raw.music)
        mp!!.start()

        if (alertTripEntity?.workRequest != -1L) {
            val calender = Calendar.getInstance()
            val currentDay = calender.get(Calendar.DAY_OF_MONTH)
            val lastDayOfMonth = calender.getActualMaximum(Calendar.DAY_OF_MONTH)
            Log.i("ccccccccc", "onCreate: $lastDayOfMonth")
            val nDay = alertTripEntity?.date!!.split(".")
            val day = nDay[0].toInt()
            val month = nDay[1].split(" ")[0].toInt()
            val year = nDay[1].split(" ")[1].toInt()
            var date = ""
            when (alertTripEntity?.workRequest) {
                1L -> {
                    date = if (currentDay == lastDayOfMonth) {
                        "${1}.${month + 1} $year"
                    } else {
                        "${day + 1}.$month $year"
                    }
                }
                30L -> {
                    date = "$day.${month + 1} $year"
                }
                365L -> {
                    date = "$day.$month ${year + 1}"
                }
            }

            viewModel.updateDate(alertTripEntity!!.tripId, date)
        }
    }

    private fun stopMediaPlayer() {
        if (mp != null) {
            mp!!.stop()
        }
    }

    private fun startTrip(tripEntity: TripEntity) {
        val intent = Intent(this, FloatingBubbleService::class.java)
        this.stopService(intent)
        val intentUri =
            Uri.parse("google.navigation:q=${tripEntity.endPointLatLng}")
        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
        if (alertTripEntity?.workRequest == -1L) {
            viewModel.updateStatus(tripEntity.tripId, "FINISHED")
        }
        viewModel.updateStartTime(tripEntity.tripId, System.currentTimeMillis())
        if (!Settings.canDrawOverlays(this)) {
            getPermission()
        } else {
            val intent = Intent(this, FloatingBubbleService::class.java)
            intent.putExtra(
                "notes",
                tripEntity.note.split(",*herewecansplitit").joinToString("\n")
            )
            intent.putExtra(
                "tripEntity", tripEntity
            )
            this.startService(intent)
        }
    }

    private fun getPermission() {
        //check for alert window permission
        //to draw overlays allowed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(
                this
            )
        ) {
            //request for drawing over apps permission
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${this.packageName}")
            )
            launcher.launch(intent)
        }
    }

    private fun startFloatingBubbleLauncher() {
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(
                        this, "permissions Denied by user", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    companion object {
        const val CHANNEL_ID = "channel_id"
    }


    private fun createNotification(trip: TripEntity) {
        Log.i("nnnnnnnnnnnnnnnnnnnnnn", "createNotification:ooooooooooooooooo ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Trip Planner"
            val channelImportance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, channelName, channelImportance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(applicationContext, AlertDialogActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("alertEntity", trip)
        intent.putExtra("music", false)
        val pendingIntent: PendingIntent =
            // Use FLAG_IMMUTABLE for targeting Android 12 (API level 31) and above
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )


        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Trip Planner")
            .setContentText("trip time, it's time to travel")
            .setSmallIcon(R.drawable.logo)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)

        // Show the notification
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notificationBuilder.build())
    }
}