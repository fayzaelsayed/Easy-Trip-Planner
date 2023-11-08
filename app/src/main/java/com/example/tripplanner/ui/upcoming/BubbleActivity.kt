package com.example.tripplanner.ui.upcoming

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.tripplanner.R
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.databinding.ActivityBubbleBinding

class BubbleActivity : AppCompatActivity() {
    lateinit var binding: ActivityBubbleBinding
    lateinit var viewModel: UpcomingTripsViewModel
   // lateinit var tripEntity: TripEntity
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bubble)
        viewModel = ViewModelProvider(this)[UpcomingTripsViewModel::class.java]


        //viewModel.showDialog()
//        val extras = intent.extras
//        if (extras != null) {
            val receivedEntity: TripEntity? = intent.getParcelableExtra<TripEntity>("tripEntity")
            viewModel.showDialog(receivedEntity!!,this)

            // Do something with the value'
            //binding.tvCaption.text= value

       // }

    }
}