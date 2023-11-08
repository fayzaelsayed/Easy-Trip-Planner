package com.example.tripplanner.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tripplanner.R
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.databinding.FragmentProfileBinding
import com.example.tripplanner.ui.authentication.AuthenticationActivity
import com.example.tripplanner.utils.GlobalHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var globalHelper: GlobalHelper
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var viewModel: ProfileViewModel
    private lateinit var upcomingTripsList: MutableList<TripEntity>
    private lateinit var finishedTripsList: MutableList<TripEntity>
    private lateinit var canceledTripsList: MutableList<TripEntity>

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        globalHelper = GlobalHelper(requireActivity())
        firebaseAuth = FirebaseAuth.getInstance()
        setHasOptionsMenu(true)
        upcomingTripsList = ArrayList()
        finishedTripsList = ArrayList()
        canceledTripsList = ArrayList()
        //val email = globalHelper.getSharedPreferences("Email", "None")
        //val name = globalHelper.getSharedPreferences("Name", "None")
//        val currentUser = auth.currentUser


//        viewModel.numberOfUpcomingTrips()
//        viewModel.countUpcoming.observe(viewLifecycleOwner){
//            it?.let {
//                binding.tvDisplayCountFinished.text = "$it"
//            }
//        }
//        viewModel.numberOfFinishedTrips()
//        viewModel.countFinished.observe(viewLifecycleOwner){
//            it?.let {
//                binding.tvDisplayCountFinished.text = "$it"
//            }
//        }
//        viewModel.numberOfCanceledTrips()
//        viewModel.countCanceled.observe(viewLifecycleOwner){
//            it?.let {
//                binding.tvDisplayCountCanceled.text = "$it"
//            }
//        }

        val email = globalHelper.getSharedPreferences("Email", "")
//        viewModel.getUpcomingTrips(email!!).observe(viewLifecycleOwner) { trips ->
//            val nUpcoming = trips.size
//            binding.tvDisplayCountUpcoming.text = "$nUpcoming"
//        }
//        viewModel.getFinishedTrips(email!!).observe(viewLifecycleOwner) { trips ->
//            val nFinished = trips.size
//            binding.tvDisplayCountFinished.text = "$nFinished"
//        }
//        viewModel.getCanceledTrips(email!!).observe(viewLifecycleOwner) { trips ->
//            val nCanceled = trips.size
//            binding.tvDisplayCountCanceled.text = "$nCanceled"
//        }

        viewModel.getAllTrips(email!!).observe(viewLifecycleOwner) {
            it?.let {
                for (trip in it) {
                    when (trip.tripStatus) {
                        "UPCOMING" -> {
                            upcomingTripsList.add(trip)
                            Log.i("bbbbbbbb", "onCreateView: ${upcomingTripsList.size}")
                            binding.tvDisplayCountUpcoming.text = "${upcomingTripsList.size}"
                        }
                        "FINISHED" -> {
                            finishedTripsList.add(trip)
                            binding.tvDisplayCountFinished.text = "${finishedTripsList.size}"
                        }
                        "CANCELED" -> {
                            canceledTripsList.add(trip)
                            binding.tvDisplayCountCanceled.text = "${canceledTripsList.size}"
                        }
                    }
                }
            }
        }





        binding.tvUserMail.text = "${firebaseAuth.currentUser?.email}"
        binding.tvUserName.text = "${firebaseAuth.currentUser?.displayName}"

        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.overflow_menu, menu)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.sign_out -> {
            GlobalScope.launch(Dispatchers.IO) {
                firebaseAuth.signOut()
                val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("648176116060-imuo0nu84acktenqcjnd04pklam4uu4u.apps.googleusercontent.com")
                    .requestEmail()
                    .build()

                googleSignInClient = GoogleSignIn.getClient(requireActivity(), options)
                googleSignInClient.signOut()
                globalHelper.setBooleanSharedPreferences("isLogged", false)
                val intent = Intent(
                    this@ProfileFragment.requireContext(),
                    AuthenticationActivity::class.java
                )
                startActivity(intent)
                requireActivity().finishAffinity()
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}