package com.example.tripplanner.ui.pasttrips


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.example.tripplanner.BuildConfig
import com.example.tripplanner.R
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.databinding.FragmentPastTripsBinding
import com.example.tripplanner.ui.upcoming.UpcomingTripsAdapter
import com.example.tripplanner.utils.GlobalHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.GeoApiContext
import com.google.maps.android.PolyUtil
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.TextHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONObject


class PastTripsFragment : Fragment(), OnMapReadyCallback {
    lateinit var binding: FragmentPastTripsBinding
    private lateinit var adapter: UpcomingTripsAdapter
    private lateinit var viewModel: PastTripsViewModel
    private lateinit var globalHelper: GlobalHelper

    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    private lateinit var context: GeoApiContext
    private var finishedTripsList: List<TripEntity>? = null
    private val routePointsList = mutableListOf<LatLng>()

    private var distanceValue = ""
    private var durationValue = ""
    private var polyLineOptions: PolylineOptions? = null
    private var polyline: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPastTripsBinding.inflate(inflater, container, false)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment =
            childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)


        globalHelper = GlobalHelper(requireActivity())
        setUpAdapter()

        viewModel = ViewModelProvider(this)[PastTripsViewModel::class.java]

        val email = globalHelper.getSharedPreferences("Email", "")
        viewModel.getUserTrips(email!!).observe(viewLifecycleOwner) { trip ->
            adapter.submitList(trip)
        }

//        binding.btnRouting.setOnClickListener {
//
//        }
        return binding.root
    }

    private fun setUpAdapter() {
        adapter = UpcomingTripsAdapter(false)
        binding.rvPastTrips.adapter = adapter
        adapter.setOnButtonClickListener(object : UpcomingTripsAdapter.OnButtonClickListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onButtonClick(
                tripEntity: TripEntity,
                action: String,
                position: Int,
                rvView: View,
                text: String
            ) {
                when (action) {
                    "route" -> {
                        doTheRequest(tripEntity)
                        binding.nestedScrollView.smoothScrollTo(0, 0)
                    }
                    "dialog" -> {
                        showDialog(text)
                    }
                    "more" -> {
                        showPopupMenu(rvView, tripEntity)
                    }

                }
            }

        })

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

    @RequiresApi(Build.VERSION_CODES.O)
    fun showPopupMenu(view: View, tripEntity: TripEntity) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.delete -> {
                    WorkManager.getInstance(requireContext().applicationContext).cancelAllWorkByTag(
                        tripEntity.tripId
                    )
                    viewModel.deleteTrip(tripEntity)
                    true
                }
                else -> {
                    false
                }
            }
        }
       popupMenu.inflate(R.menu.item_menu)
        popupMenu.menu.findItem(R.id.editText).isVisible = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true)
        }
        popupMenu.show()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map!!
        val email = globalHelper.getSharedPreferences("Email", "")
        viewModel.getFinishedTrips(email!!).observe(viewLifecycleOwner) { trips ->
            if (trips.isNotEmpty()){
                finishedTripsList = trips
                doTheRequest(finishedTripsList!![0])
                Log.i("kkkkkkkkkkkkkkkkkkkkkkkkk", "onMapReady: $finishedTripsList")
            }else{
                googleMap = map
            }

        }
        Log.i("bbbbbbbbbbbbbbbb", "onMapReady: map ")
    }

    private fun doTheRequest(tripEntity: TripEntity) {
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=${tripEntity.startPointLatLng}&destination=${tripEntity.endPointLatLng}&key=${BuildConfig.api_key}"
        val asyncHttpClient = AsyncHttpClient()
        asyncHttpClient.get(url, object : TextHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>?,
                responseString: String?
            ) {
                polyline?.remove()
                startMarker?.remove()
                endMarker?.remove()
                drawPolyline(responseString, tripEntity)
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseString: String?,
                throwable: Throwable?
            ) {
                Toast.makeText(requireContext(), "Failed to Draw on the Map", Toast.LENGTH_LONG)
                    .show()
            }

        })
    }

    fun drawPolyline(response: String?, tripEntity: TripEntity) {
        val jsonObject = JSONObject(response)
        val route = jsonObject.getJSONArray("routes")
        val elementOne = route.getJSONObject(0)

        val legs = elementOne.getJSONArray("legs")
        val elementNumOne = legs.getJSONObject(0)
        val distance = elementNumOne.getJSONObject("distance")
        distanceValue = distance.getString("text")
        val duration = elementNumOne.getJSONObject("duration")
        durationValue = duration.getString("text")

        val overviewPolyline = elementOne.getJSONObject("overview_polyline")
        val points = overviewPolyline.getString("points")

        val listOfPoints = PolyUtil.decode(points)

        val randomColor = Color.rgb(
            (0..255).random(),
            (0..255).random(),
            (0..255).random()
        )

        polyLineOptions = PolylineOptions()
            .color(Color.BLACK)
            .width(8f)
            .addAll(listOfPoints)

        // polyLineOptions!!.addAll(listOfPoints)

        polyline = googleMap.addPolyline(polyLineOptions!!)

        val originListLatLng = tripEntity.startPointLatLng.split(",").map { it.toDouble() }
        val origin = LatLng(originListLatLng[0], originListLatLng[1])
        val destinationListLatLng = tripEntity.endPointLatLng.split(",").map { it.toDouble() }
        val destination = LatLng(destinationListLatLng[0], destinationListLatLng[1])
//        val cameraPosition = CameraPosition.Builder()
//            .target(origin)
//            .zoom(f)
//            .build()
        val latLngBound = LatLngBounds.Builder().include(origin).include(destination).build()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 0))
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(googleMap.cameraPosition.zoom - 2))

//        googleMap.addMarker(MarkerOptions().position(origin).title("Start point of ${tripEntity.tripName}"))
        endMarker = googleMap.addMarker(
            MarkerOptions().position(destination).title("End Point of: ${tripEntity.tripName}")
        )


        val startMarker = MarkerOptions()
            .position(origin)
            .icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(tripEntity)))
            .title("Start Point of: ${tripEntity.tripName} \\n $distanceValue \\n $durationValue")
        // .snippet("${tripEntity.tripName} \n $distanceValue \n $durationValue")

        this.startMarker = googleMap.addMarker(startMarker)

//        val endMarker =  MarkerOptions()
//            .position(destination)
//            .icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(tripEntity)))
//            .title("End Point of: ${tripEntity.tripName}")
//            .snippet("This is a custom marker.")
//        googleMap.addMarker(endMarker)


    }

    @SuppressLint("MissingInflatedId")
    private fun createCustomMarker(tripEntity: TripEntity): Bitmap {
        val markerView = LayoutInflater.from(requireContext()).inflate(R.layout.view_on_map, null)
        var textViewTripName = markerView.findViewById<TextView>(R.id.tv_trip_name_on_the_map)
        var textViewTripDistance = markerView.findViewById<TextView>(R.id.tv_trip_distance)
        var textViewTripDuration = markerView.findViewById<TextView>(R.id.tv_trip_duration)
        var imag = markerView.findViewById<ImageView>(R.id.im_map)

        textViewTripName.text = "Start Point of: ${tripEntity.tripName}"
        textViewTripDistance.text = distanceValue
        textViewTripDuration.text = durationValue
        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            markerView.measuredWidth,
            markerView.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)

        return bitmap
    }

}