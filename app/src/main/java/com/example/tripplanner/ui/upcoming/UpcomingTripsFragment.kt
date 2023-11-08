package com.example.tripplanner.ui.upcoming

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import com.example.tripplanner.R
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.databinding.FragmentUpcomingTripsBinding
import com.example.tripplanner.ui.home.HomeFragmentDirections
import com.example.tripplanner.utils.GlobalHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UpcomingTripsFragment : Fragment() {
    private lateinit var binding: FragmentUpcomingTripsBinding
    private lateinit var viewModel: UpcomingTripsViewModel
    private lateinit var globalHelper: GlobalHelper
    private lateinit var rvAdapter: UpcomingTripsAdapter
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var dialogAdapter: NotesCheckListAdapter
    private lateinit var workList: MutableList<TripEntity>


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val themedContext = ContextThemeWrapper(requireContext(), R.style.Theme_TripPlanner)
        binding = FragmentUpcomingTripsBinding.inflate(inflater, container, false)
        globalHelper = GlobalHelper(requireActivity())
        setUpAdapter()
        viewModel = ViewModelProvider(this)[UpcomingTripsViewModel::class.java]

        viewModel.getTripsFromFireStore()

        viewModel.showProgress.observe(viewLifecycleOwner) {
            it?.let {
                if (it) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvUpcomingTrips.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.rvUpcomingTrips.visibility = View.VISIBLE
                }
            }
        }
        val email = globalHelper.getSharedPreferences("Email", "")

        workList = ArrayList()
        viewModel.getUserTrips(email!!).observe(viewLifecycleOwner) { trips ->
            trips?.let {
                for (trip in trips) {
                    if (trip.source == "FIREBASE") {
                        try {
                            val currentDateTime = LocalDateTime.now()
                            val combinedString = trip.date + " " + trip.time
                            val dateTimeToCompare = LocalDateTime.parse(
                                combinedString,
                                DateTimeFormatter.ofPattern("d.M yyyy HH:mm")
                            )
                            if (currentDateTime.isBefore(dateTimeToCompare)) {
                                workList.add(trip)
                            } else {
                                viewModel.updateStatus(trip.tripId, "CANCELED")
                            }
                        } catch (e: Exception) {
                            Log.i("kkkkkkkkkkkkkkkkkkkkkkk", "onCreateView: $e ")
                        }
                    }
                }
                for (i in workList) {
                    viewModel.myWorkManagerRequest(requireContext(), i, i.workRequest)
                }
                rvAdapter.submitList(trips)

            }
        }
        startFloatingBubbleLauncher()
        getPermission()

        return binding.root
    }


    private fun setUpAdapter() {
        rvAdapter = UpcomingTripsAdapter()
        rvAdapter.setOnButtonClickListener(object : UpcomingTripsAdapter.OnButtonClickListener {
            @RequiresApi(Build.VERSION_CODES.Q)
            @SuppressLint("NotifyDataSetChanged")
            override fun onButtonClick(
                tripEntity: TripEntity, action: String, position: Int, rvView: View, text: String
            ) {
                when (action) {
                    "start" -> {
                        val intent = Intent(requireContext(), FloatingBubbleService::class.java)
                        requireContext().stopService(intent)
                        val intentUri =
                            Uri.parse("google.navigation:q=${tripEntity.endPointLatLng}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        startActivity(mapIntent)
                        viewModel.updateStatus(tripEntity.tripId, "FINISHED")
                        viewModel.updateStartTime(tripEntity.tripId, System.currentTimeMillis())
                        WorkManager.getInstance(requireContext().applicationContext)
                            .cancelAllWorkByTag(
                                tripEntity.tripId
                            )
                        if (!Settings.canDrawOverlays(requireContext())) {
                            getPermission()
                        } else {
                            val intent = Intent(requireContext(), FloatingBubbleService::class.java)
                            intent.putExtra(
                                "notes",
                                tripEntity.note.split(",*herewecansplitit").joinToString("\n")
                            )
                            intent.putExtra(
                                "tripEntity", tripEntity
                            )
                            requireContext().startService(intent)
                        }


                    }
                    "more" -> {
                        showPopupMenu(rvView, tripEntity)

                    }
                    "dialog" -> {
                        showDialog(text)
                    }
                }
            }
        })
        binding.rvUpcomingTrips.adapter = rvAdapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showPopupMenu(view: View, tripEntity: TripEntity) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.editText -> {
                    findNavController().navigate(
                        HomeFragmentDirections.actionMainFragmentToAddTripFragment(
                            tripEntity
                        )
                    )
                    true
                }
                R.id.delete -> {
                    if (tripEntity.tripType == "SINGLE") {
                        WorkManager.getInstance(requireContext().applicationContext)
                            .cancelAllWorkByTag(
                                tripEntity.tripId
                            )
                        viewModel.deleteTrip(tripEntity)
                    } else if (tripEntity.tripType == "ROUNDED") {
                        WorkManager.getInstance(requireContext().applicationContext)
                            .cancelAllWorkByTag(
                                tripEntity.tripId
                            )
                        viewModel.deleteTrip(tripEntity)
                        val email = globalHelper.getSharedPreferences("Email", "")

                        viewModel.getUserTrips(email!!)
                            .observe(viewLifecycleOwner) {
                                it?.let {
                                    for (i in it){
                                        if (i.roundedId == tripEntity.tripId){
                                            viewModel.deleteTrip(i)
                                        }
                                    }
                                }
                            }

                    }
                    true
                }
                else -> {
                    false
                }
            }
        }
        popupMenu.inflate(R.menu.item_menu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true)
        }
        popupMenu.show()
    }

    private fun startFloatingBubbleLauncher() {
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (!Settings.canDrawOverlays(requireContext())) {
                    Toast.makeText(
                        requireContext(), "permissions Denied by user", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getPermission() {
        //check for alert window permission
        //to draw overlays allowed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(
                requireContext()
            )
        ) {
            //request for drawing over apps permission
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            launcher.launch(intent)
        }
    }

    private fun showDialog(text: String) {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_show_list, null)
        val textView = dialogLayout.findViewById<TextView>(R.id.tv_notes_list)
        val okButton = dialogLayout.findViewById<Button>(R.id.btn_ook)

        textView.text = text

        val builder = AlertDialog.Builder(requireContext()).setView(dialogLayout).create()

        okButton.setOnClickListener {
            builder.dismiss()
        }

        builder.show()
    }
}