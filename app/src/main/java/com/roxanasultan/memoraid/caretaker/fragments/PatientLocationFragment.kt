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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_patient_location, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
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
                    loadCurrentRoute(patient.id)
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
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 1001)
            return
        }
        map.isMyLocationEnabled = true
        map.setOnMarkerClickListener { marker ->
            marker.snippet?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
            false
        }
        if (userViewModel.patient.value != null) {
            loadPatientLocation()
            loadCurrentRoute(userViewModel.patient.value!!.id)
        }
    }

    private fun loadPatientLocation() {
        val patient = userViewModel.patient.value
        val location = patient?.location ?: return
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

    private fun loadCurrentRoute(patientId: String) {
        FirebaseFirestore.getInstance().collection("users").document(patientId).get()
            .addOnSuccessListener { document ->
                val route = document["currentRoute"] as? List<Map<String, Any>> ?: return@addOnSuccessListener
                val points = mutableListOf<LatLng>()
                for (entry in route) {
                    val geoPoint = entry["geoPoint"] as? GeoPoint ?: continue
                    val arrival = entry["arrivalTime"] as? String
                    val duration = (entry["duration"] as? Number)?.toLong()

                    val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                    points.add(latLng)

                    val timeStr = try {
                        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                        format.timeZone = TimeZone.getTimeZone("UTC")
                        val parsedDate = format.parse(arrival ?: "") ?: Date()
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsedDate)
                    } catch (e: Exception) { "Unknown" }

                    val durationStr = duration?.let { "${it / 60} min" } ?: "N/A"

                    map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Stop")
                            .snippet("Arrived at $timeStr\nStayed $durationStr")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    )
                }
                if (points.size > 1) {
                    map.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .color(resources.getColor(R.color.red, null))
                            .width(8f)
                    )
                }
            }
            .addOnFailureListener {
                Log.e("RouteLoad", "Failed to load route: ${it.message}", it)
            }
    }

    private fun getResizedPatientIcon(resourceId: Int, width: Int, height: Int): BitmapDescriptor {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (::map.isInitialized) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    map.isMyLocationEnabled = true
                }
            }
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}