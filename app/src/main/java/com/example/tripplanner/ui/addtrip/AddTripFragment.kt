package com.example.tripplanner.ui.addtrip

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.example.tripplanner.BuildConfig
import com.example.tripplanner.R
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.databinding.FragmentAddTripBinding
import com.example.tripplanner.utils.GlobalHelper
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class AddTripFragment : Fragment(), View.OnFocusChangeListener {
    private lateinit var binding: FragmentAddTripBinding
    private lateinit var viewModel: AddTripViewModel
    private lateinit var globalHelper: GlobalHelper
    private lateinit var placesClient: PlacesClient
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var arguments: AddTripFragmentArgs
    private var hyear = 0
    private var hmonth = 0
    private var hday = 0
    private var hHour = 0
    private var hMinute = 0
    private var startPointLatLng = ""
    private var sLat = 0.0
    private var sLng = 0.0
    private var endPointLatLng = ""
    private var eLat = 0.0
    private var eLng = 0.0
    private var startPoint: Boolean = true

    private var distanceValue = ""
    private var durationValue = ""

    private lateinit var dialogAdapter: NotesAdapter
    private val slist = mutableListOf<String>()
    private lateinit var list: MutableList<String>
    private lateinit var roundedList: MutableList<String>

    private var notess = ""
    private var roundedNotes = ""
    private var numberOfWork = -1L
    private var tripType = "SINGLE"

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddTripBinding.inflate(inflater, container, false)

        globalHelper = GlobalHelper(requireActivity())

//        val application = requireNotNull(this.activity).application
//        val viewModelFactory = AddTripViewModelFactory(application)
        viewModel = ViewModelProvider(this)[AddTripViewModel::class.java]
        list = ArrayList()
        roundedList = ArrayList()

        Places.initialize(requireContext(), BuildConfig.api_key)
        placesClient = Places.createClient(requireContext())

        arguments = AddTripFragmentArgs.fromBundle(requireArguments())

        startAutoCompleteLauncher()



        viewModel.progressBarIsVisible.observe(viewLifecycleOwner) {
            if (!it) {
                binding.pbAddTrip.visibility = View.GONE
            } else {
                binding.pbAddTrip.visibility = View.VISIBLE
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                observeErrorMessage(errorMessage)
            }
        }



        viewModel.distanceValue.observe(viewLifecycleOwner) { distanceValueFromRequest ->
            distanceValueFromRequest?.let {
                if (distanceValueFromRequest.isNotEmpty()) {
                    distanceValue = distanceValueFromRequest
                } else {
//                    distanceValue = "Not Available"
                }
            }
        }

        viewModel.durationValue.observe(viewLifecycleOwner) { durationValueFromRequest ->
            durationValueFromRequest?.let {
                if (durationValueFromRequest.isNotEmpty()) {
                    durationValue = durationValueFromRequest
                } else {
//                durationValue = "Not Available"
                }
            }

        }

        viewModel.requestSuccess.observe(viewLifecycleOwner) { requestSuccess ->
            requestSuccess?.let {
                if (requestSuccess) {
                    insertToDatabaseAndFirebase()
                } else {
                    insertToDatabaseAndFirebase()
                    Toast.makeText(
                        requireContext(),
                        "Failed to Calculate Distance and Duration",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


        val calender = Calendar.getInstance()
        val lastDayOfMonth = calender.getActualMaximum(Calendar.DAY_OF_MONTH)
        binding.radioGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.rb_once -> numberOfWork = -1L
                R.id.rb_repeat_daily -> numberOfWork = 1L
                R.id.rb_repeat_monthly -> numberOfWork = lastDayOfMonth.toLong()
                R.id.rb_repeat_yearly -> numberOfWork = 365L
            }
        }

        binding.swRounded.setOnCheckedChangeListener { _, isOn ->
            if (isOn) {
                tripType = "ROUNDED"
                binding.clRounded.visibility = View.VISIBLE
            } else {
                tripType = "SINGLE"
                binding.clRounded.visibility = View.GONE
            }
        }




        binding.apply {
            ibCalender.setOnClickListener {
                openDatePicker(binding.tvDate)
            }
            ibCalenderRounded.setOnClickListener {
                openDatePicker(binding.tvDateRounded)
            }

            ibTime.setOnClickListener {
                handleTheDateFirst(binding.tvDate, binding.tvTime)
            }

            ibTimeRounded.setOnClickListener {
                handleTheDateFirst(binding.tvDateRounded, binding.tvTimeRounded)
            }

            btnAddTrip.setOnClickListener {
                if (arguments.currentTrip != null) {
                    updateTrip(arguments.currentTrip!!)
                    WorkManager.getInstance(requireContext().applicationContext).cancelAllWorkByTag(
                        arguments.currentTrip!!.tripId
                    )
                    binding.swRounded.isEnabled = false
                    binding.clRounded.visibility = View.GONE
                    viewModel.myWorkManagerRequest(
                        hmonth, hday, hHour, hMinute, requireContext(),
                        arguments.currentTrip!!, numberOfWork
                    )

                } else {
                    if (isDateInputValid()) {
                        // request distance then insert trip data after response
                        viewModel.requestingForDistanceAndDuration(startPointLatLng, endPointLatLng)
                    }
                }
            }


            tvNotes.setOnClickListener {
                showDialog(list, tvNotes)
            }

            tvNotesRounded.setOnClickListener {
                showDialog(roundedList, tvNotesRounded)
            }

            edtTripName.onFocusChangeListener = this@AddTripFragment
            edtStartPoint.onFocusChangeListener = this@AddTripFragment
            edtEndPoint.onFocusChangeListener = this@AddTripFragment
            ibCalender.onFocusChangeListener = this@AddTripFragment
        }

//        binding.edtStartPoint.setOnClickListener {
//            startAutocompleteActivity()
//        }

        if (arguments.currentTrip != null) {
            fillOutTheViews(arguments.currentTrip!!)

        }

        return binding.root
    }

    private fun handleTheDateFirst(textViewDate: TextView, textViewTime: TextView) {
        if (textViewDate.text.toString().isEmpty()) {
            Toast.makeText(requireContext(), "Pick The date first", Toast.LENGTH_LONG)
                .show()
        } else {
            openTimePicker(textViewTime)
        }
    }


    override fun onFocusChange(view: View, hasFocus: Boolean) {
        when (view.id) {
            R.id.edt_trip_name -> {
                if (hasFocus) {
                    if (binding.tvTripName.isErrorEnabled) {
                        binding.tvTripName.isErrorEnabled = false
                    }
                } else {
                    val tripName = binding.edtTripName.text.toString()
                    viewModel.validTripName(tripName)
                }
            }

            R.id.edt_start_point -> {
                if (hasFocus) {
                    startAutocompleteActivity()
                    startPoint = true
                    if (binding.tvStartPoint.isErrorEnabled) {
                        binding.tvStartPoint.isErrorEnabled = false
                    }
                } else {
                    val startPoint = binding.edtStartPoint.text.toString()
                    viewModel.validStartPoint(startPoint)
                }
            }

            R.id.edt_end_point -> {
                if (hasFocus) {
                    startAutocompleteActivity()
                    startPoint = false
                    if (binding.tvEndPoint.isErrorEnabled) {
                        binding.tvEndPoint.isErrorEnabled = false
                    }
                } else {
                    val endPoint = binding.edtEndPoint.text.toString()
                    viewModel.validEndPoint(endPoint)
                }
            }
        }
    }


    private fun observeErrorMessage(errorMessage: String) {
        when (errorMessage) {
            "Trip Name Is Required" -> binding.tvTripName.apply {
                isErrorEnabled = true
                error = errorMessage
            }
            "Start Point Is Required" -> binding.tvStartPoint.apply {
                isErrorEnabled = true
                error = errorMessage
            }
            "End Point Is Required" -> binding.tvEndPoint.apply {
                isErrorEnabled = true
                error = errorMessage
            }
        }
    }

    private fun startAutoCompleteLauncher() {
        launcher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    if (startPoint) {
                        val place = Autocomplete.getPlaceFromIntent(it.data!!)
                        binding.edtStartPoint.setText(place.name)
                        binding.edtStartPoint.clearFocus()
                        val latLng = place.latLng!!.toString()
                        startPointLatLng = latLng.substringAfter('(').substringBefore(')')
                        sLat = place.latLng.latitude
                        sLng = place.latLng.longitude
                    } else {
                        val place = Autocomplete.getPlaceFromIntent(it.data!!)
                        binding.edtEndPoint.setText(place.name)
                        binding.edtEndPoint.clearFocus()
                        val latLng = place.latLng!!.toString()
                        endPointLatLng = latLng.substringAfter('(').substringBefore(')')
                        eLat = place.latLng.latitude
                        eLng = place.latLng.longitude
                    }
                } else if (it.resultCode == AutocompleteActivity.RESULT_ERROR) {
                    val status = Autocomplete.getStatusFromIntent(it.data!!)
                    Toast.makeText(requireContext(), status.statusMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    //Another way instead of launcher but deprecated
    //    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
//            val place = Autocomplete.getPlaceFromIntent(data!!)
//            binding.edtStartPoint.setText(place.name)
//            binding.edtStartPoint.clearFocus()
//        } else if (requestCode == 300 && resultCode == Activity.RESULT_OK) {
//            val place = Autocomplete.getPlaceFromIntent(data!!)
//            binding.edtEndPoint.setText(place.name)
//            binding.edtEndPoint.clearFocus()
//            val latLng = place.latLng.toString()
//            usedLatLng = latLng.substringAfter('(').substringBefore(')')
//
//        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
//            val status = Autocomplete.getStatusFromIntent(data!!)
//            Toast.makeText(requireContext(), status.statusMessage, Toast.LENGTH_LONG).show()
//        }
//    }

    private fun startAutocompleteActivity() {
        val fields =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountries(listOf("EG")).setTypesFilter(listOf(PlaceTypes.ADDRESS))
            .build(requireContext())
        // startActivityForResult(intent, requestCode)
        launcher.launch(intent)
    }


    private fun openDatePicker(textView: TextView) {
        val calender = Calendar.getInstance()
        val year = calender.get(Calendar.YEAR)
        val month = calender.get(Calendar.MONTH)
        val day = calender.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, mYear, mMonth, mDay ->
                textView.text = "$mDay.${mMonth + 1} $mYear"
                hyear = mYear
                hmonth = mMonth
                hday = mDay
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
    }

    @SuppressLint("SimpleDateFormat")
    private fun openTimePicker(textView: TextView) {
        val calender = Calendar.getInstance()
        val timePickerDialogListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            calender.set(
                hyear,
                hmonth,
                hday,
                hour,
                minute
            )
            hHour = hour
            hMinute = minute
            if (calender.timeInMillis >= Calendar.getInstance().timeInMillis) {
                textView.text = SimpleDateFormat("HH:mm").format(calender.time)

            } else {
                Toast.makeText(requireContext(), "time unavailable", Toast.LENGTH_LONG).show()
            }

        }
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            timePickerDialogListener,
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun isDateInputValid(): Boolean {
        var isChosen = false
        if (binding.tvDate.text.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Date is Required",
                Toast.LENGTH_LONG
            ).show()
        } else if (binding.tvTime.text.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Time is Required",
                Toast.LENGTH_LONG
            ).show()
        } else if (tripType == "ROUNDED") {
            if (binding.tvDateRounded.text.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Back Trip Date is Required",
                    Toast.LENGTH_LONG
                ).show()
            } else if (binding.tvTimeRounded.text.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Back Trip Time is Required",
                    Toast.LENGTH_LONG
                ).show()
            }else if (isBackTripBeforeOriginal()) {
                Toast.makeText(
                    requireContext(),
                    "The back trip date must be after the original trip",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                isChosen = true
            }
        } else {
            isChosen = true
        }
        return isChosen
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isBackTripBeforeOriginal(): Boolean {
        val originalTripDate = binding.tvDate.text.toString() + " " + binding.tvTime.text.toString()
        val originalDateFormatted =
            LocalDateTime.parse(originalTripDate, DateTimeFormatter.ofPattern("d.M yyyy HH:mm"))
        val backTripDate =
            binding.tvDateRounded.text.toString() + " " + binding.tvTimeRounded.text.toString()
        val backDateFormatted =
            LocalDateTime.parse(backTripDate, DateTimeFormatter.ofPattern("d.M yyyy HH:mm"))
        return backDateFormatted.isBefore(originalDateFormatted)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun insertToDatabaseAndFirebase() {
        if (list.isNotEmpty()) {
            notess = list.joinToString(separator = ",*herewecansplitit")
            roundedNotes = roundedList.joinToString(separator = ",*herewecansplitit")
        }
        val trip = TripEntity(
            tripId = UUID.randomUUID().toString(),
            tripName = binding.edtTripName.text.toString(),
            startPoint = binding.edtStartPoint.text.toString(),
            endPoint = binding.edtEndPoint.text.toString(),
            note = notess,
            date = binding.tvDate.text.toString(),
            time = binding.tvTime.text.toString(),
            email = globalHelper.getSharedPreferences("Email", "")!!,
            "UPCOMING",
            startPointLatLng,
            endPointLatLng,
            insertionTime = System.currentTimeMillis(),
            distanceValue,
            durationValue,
            startTime = 0L,
            workRequest = numberOfWork,
            source = "DATABASE",
            tripType = tripType,
            roundedId = ""
        )

        if (tripType == "SINGLE") {
            viewModel.insertTripToDatabaseAndFirebase(trip)
        } else if (tripType == "ROUNDED") {
            CoroutineScope(Dispatchers.Main).launch {
                delay(200)
                val backTrip = TripEntity(
                    tripId = UUID.randomUUID().toString(),
                    tripName = binding.edtTripName.text.toString(),
                    startPoint = binding.edtEndPoint.text.toString(),
                    endPoint = binding.edtStartPoint.text.toString(),
                    note = roundedNotes,
                    date = binding.tvDateRounded.text.toString(),
                    time = binding.tvTimeRounded.text.toString(),
                    email = globalHelper.getSharedPreferences("Email", "")!!,
                    "UPCOMING",
                    endPointLatLng,
                    startPointLatLng,
                    insertionTime = System.currentTimeMillis(),
                    distanceValue,
                    durationValue,
                    startTime = 0L,
                    workRequest = numberOfWork,
                    source = "DATABASE",
                    tripType = "SINGLE",
                    roundedId = trip.tripId
                )
                viewModel.insertTripToDatabaseAndFirebase(trip)
                viewModel.insertTripToDatabaseAndFirebase(backTrip)
            }

        }
        Toast.makeText(requireContext(), "Trip has been added successfully", Toast.LENGTH_LONG)
            .show()
        viewModel.myWorkManagerRequest(
            hmonth,
            hday,
            hHour,
            hMinute,
            requireContext(),
            trip,
            numberOfWork
        )
        findNavController().navigate(R.id.action_addTripFragment_to_mainFragment)
    }

    @SuppressLint("SetTextI18n")
    private fun fillOutTheViews(currentTrip: TripEntity) {
        binding.swRounded.visibility = View.GONE
        val x = currentTrip.note.split(",*herewecansplitit").toList().joinToString("\n")

        list = currentTrip.note.split(",*herewecansplitit").toMutableList()

        val nDay = currentTrip.date.split(".")
        hday = nDay[0].toInt()
        hmonth = nDay[1].split(" ")[0].toInt() - 1
        hyear = nDay[1].split(" ")[1].toInt()
        binding.edtTripName.setText(currentTrip.tripName)
        binding.edtStartPoint.setText(currentTrip.startPoint)
        binding.edtEndPoint.setText(currentTrip.endPoint)
        binding.tvNotes.text = x
        binding.tvDate.text = currentTrip.date
        binding.tvTime.text = currentTrip.time
        binding.btnAddTrip.text = "Save"

        binding.tvNotesRounded.text = x
    }

    private fun updateTrip(currentTrip: TripEntity) {
        if (list.isNotEmpty()) {
            notess = list.joinToString(separator = ",*herewecansplitit")
        }
        currentTrip.tripName = binding.edtTripName.text.toString()
        currentTrip.startPoint = binding.edtStartPoint.text.toString()
        currentTrip.endPoint = binding.edtEndPoint.text.toString()
        currentTrip.note = notess
        currentTrip.date = binding.tvDate.text.toString()
        currentTrip.time = binding.tvTime.text.toString()

        val dataMap = hashMapOf<String, Any>(
            "tripName" to currentTrip.tripName,
            "startPoint" to currentTrip.startPoint,
            "endPoint" to currentTrip.endPoint,
            "note" to currentTrip.note,
            "date" to currentTrip.date,
            "time" to currentTrip.time
        )

        val db = Firebase.firestore
        val user = Firebase.auth.currentUser
        val subCollectionRef =
            user?.let { db.collection("users").document(user.uid).collection("trips") }
        subCollectionRef?.document(currentTrip.tripId)?.update(dataMap!!)
        viewModel.updateTrip(currentTrip)
        Toast.makeText(requireContext(), "Trip has been updated successfully", Toast.LENGTH_LONG)
            .show()
        findNavController().navigate(R.id.action_addTripFragment_to_mainFragment)
    }

    private fun showDialog(inputList: MutableList<String>, textView: TextView) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_list)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCancelable(true)

        val addSNoteEditText = dialog.findViewById<EditText>(R.id.edt_add_note)
        val addNoteButton = dialog.findViewById<Button>(R.id.btn_add_note)
        val saveDialogButton = dialog.findViewById<Button>(R.id.btn_save_list)
        val cancelDialogButton = dialog.findViewById<Button>(R.id.btn_cancel_dialog)
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.rv_list_dialog)


        dialogAdapter = NotesAdapter()
        if (inputList.isNotEmpty()) {
            dialogAdapter.submitList(inputList)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = dialogAdapter
        dialogAdapter.setOnButtonClickListener(object : NotesAdapter.OnButtonClickListener {
            override fun onButtonClick(position: Int) {
                inputList.removeAt(position)
                dialogAdapter.notifyDataSetChanged()
            }
        })

        addNoteButton.setOnClickListener {
            val note = addSNoteEditText.text.toString().trim()
            if (note.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "You have to enter the note to add it",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                inputList.add(note)
                dialogAdapter.submitList(inputList)
                dialogAdapter.notifyDataSetChanged()
                addSNoteEditText.text.clear()
            }
        }

        saveDialogButton.setOnClickListener {
            textView.text = inputList.joinToString("\n")
            dialog.dismiss()
        }

        cancelDialogButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}