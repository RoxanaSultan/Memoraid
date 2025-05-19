package com.roxanasultan.memoraid.caretaker.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.viewmodels.UserViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PatientLocationFragment : Fragment(), OnMapReadyCallback {

    private val userViewModel: UserViewModel by viewModels()

    private lateinit var map: GoogleMap
    private var lastTappedMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_patient_location, container, false)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        userViewModel.loadUser()
        userViewModel.loadPatient()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            userViewModel.patient.collectLatest { patient ->
                if (::map.isInitialized && patient != null) {
                    loadPatientLocation()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
            return
        }

        map.isMyLocationEnabled = true

        map.setOnMarkerClickListener { marker ->
            if (marker.title == "Patient's Location") {
                val location = marker.position
                Toast.makeText(
                    requireContext(),
                    "Patient is here: ${getAddressFromLocation(location)}",
                    Toast.LENGTH_LONG
                ).show()
            }
            true
        }

        map.setOnMapClickListener { latLng ->
            lastTappedMarker?.remove()

            val addressText = getAddressFromLocation(latLng)

            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(addressText ?: "Unknown Location")
            )

            lastTappedMarker = marker

            Toast.makeText(
                requireContext(),
                "Selected Location: ${addressText ?: "Address not found"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        loadPatientLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (::map.isInitialized) {
                    if (
                        ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ) {
                        map.isMyLocationEnabled = true
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getAddressFromLocation(latLng: LatLng): String? {
        return try {
            val geocoder = Geocoder(requireContext())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                address.getAddressLine(0)
            } else null
        } catch (e: Exception) {
            Log.e("Geocoder", "Error getting address", e)
            null
        }
    }

    private fun loadPatientLocation() {
        val patient = userViewModel.patient.value
        val location = patient?.location
        val photoUrl = patient?.profilePictureUrl

        if (location != null && !photoUrl.isNullOrEmpty()) {
            val latLng = LatLng(location.latitude, location.longitude)

            Glide.with(this)
                .asBitmap()
                .load(photoUrl)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val resizedBitmap = Bitmap.createScaledBitmap(resource, 115, 115, false)
                        val descriptor =
                            BitmapDescriptorFactory.fromBitmap(
                                resizedBitmap
                            )

                        map.clear()
                        map.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title("Patient's Location")
                                .icon(descriptor)
                        )
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        } else {
            Log.d("PatientLocation", "The patient does not have a location or profile picture.")
        }
    }
}