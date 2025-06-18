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
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PatientLocationFragment : Fragment(), OnMapReadyCallback {

    private val userViewModel: UserViewModel by viewModels()
    private lateinit var map: GoogleMap
    private var patientMarker: Marker? = null
    private var patientIcon: BitmapDescriptor? = null
    private var clickedMarker: Marker? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_patient_location, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        userViewModel.loadPatient()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            userViewModel.patient.collectLatest { patient ->
                if (patient != null && patient.id.isNotEmpty()) {
                    userViewModel.observePatientLocation(patient.id)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            userViewModel.patientLocation.collectLatest { location ->
                if (::map.isInitialized && location != null) {
                    updatePatientMarker(location.latitude, location.longitude)
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        map.isMyLocationEnabled = true
        patientIcon = getResizedPatientIcon(R.drawable.patient_icon, 100, 100)

        map.setOnMarkerClickListener { marker ->
            val latLng = marker.position
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0].getAddressLine(0)
                    marker.title = address
                    marker.showInfoWindow()
                } else {
                    marker.title = "Unknown location"
                    marker.showInfoWindow()
                }
            } catch (e: Exception) {
                marker.title = "Error getting address"
                marker.showInfoWindow()
                Log.e("Geocoding", "Failed to get address: ${e.message}")
            }
            true
        }
    }

    private fun updatePatientMarker(lat: Double, lng: Double) {
        val latLng = LatLng(lat, lng)

        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val address = try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Patient's Location"
        } catch (e: Exception) {
            Log.e("Geocoding", "Error: ${e.message}")
            "Patient's Location"
        }

        if (patientMarker == null) {
            patientMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(address)
                    .icon(patientIcon)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        } else {
            patientMarker?.position = latLng
            patientMarker?.title = address
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    private fun getResizedPatientIcon(resourceId: Int, width: Int, height: Int): BitmapDescriptor {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (::map.isInitialized) map.isMyLocationEnabled = true
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}