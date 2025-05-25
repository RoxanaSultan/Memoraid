package com.roxanasultan.memoraid.caretaker.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.viewmodels.UserViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PatientLocationFragment : Fragment(), OnMapReadyCallback {

    private val userViewModel: UserViewModel by viewModels()

    private lateinit var map: GoogleMap
    private var lastTappedMarker: Marker? = null
    private var patientMarker: Marker? = null
    private var patientIcon: BitmapDescriptor? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_patient_location, container, false)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        userViewModel.loadUser()
        userViewModel.loadPatient()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            userViewModel.patient.collectLatest { patient ->
                if (patient != null && patient.id.isNotEmpty()) {
                    userViewModel.observePatientLocation(patient.id)
                    loadPatientLocation()
                }
            }
        }

        lifecycleScope.launch {
            userViewModel.patientLocation.collectLatest { location ->
                if (::map.isInitialized && location != null) {
                    updatePatientMarker(location.latitude, location.longitude)
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
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
                true
            } else {
                false
            }
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

        if (userViewModel.patient.value != null) {
            loadPatientLocation()
        }
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
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else null
        } catch (e: Exception) {
            Log.e("Geocoder", "Error getting address", e)
            null
        }
    }

    private fun loadPatientLocation() {
        val patient = userViewModel.patient.value
        val location = patient?.location

        if (location != null) {
            val latLng = LatLng(location.latitude, location.longitude)

            patientIcon = getResizedPatientIcon(R.drawable.patient_icon, 80, 80)

            patientMarker?.remove()
            patientMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Patient's Location")
                    .icon(patientIcon)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

        } else {
            Log.d("PatientLocation", "The patient does not have a location.")
        }
    }

    private fun updatePatientMarker(lat: Double, lng: Double) {
        val latLng = LatLng(lat, lng)

        if (patientMarker == null) {
            patientMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Patient's Location")
                    .icon(patientIcon ?: getResizedPatientIcon(R.drawable.patient_icon, 80, 80))
            )
        } else {
            patientMarker?.position = latLng
        }

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
    }

    private fun getResizedPatientIcon(resourceId: Int, width: Int, height: Int): BitmapDescriptor {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
    }
}