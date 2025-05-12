package com.example.memoraid.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.memoraid.R
import com.example.memoraid.models.User
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PatientLocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var lastTappedMarker: com.google.android.gms.maps.model.Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_patient_location, container, false)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
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
            if (marker.title == "Locația pacientului") {
                val location = marker.position
                Toast.makeText(
                    requireContext(),
                    "Pacientul este aici: ${getAddressFromLocation(location)}",
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
                    .title(addressText ?: "Loc necunoscut")
            )

            lastTappedMarker = marker

            Toast.makeText(
                requireContext(),
                "Loc selectat: ${addressText ?: "Adresă indisponibilă"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        fetchSelectedPatientLocation()
    }

    private fun fetchSelectedPatientLocation() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { snapshot ->
                val currentUser = snapshot.toObject(User::class.java)
                val selectedPatientId = currentUser?.selectedPatient

                if (!selectedPatientId.isNullOrEmpty()) {
                    loadPatientLocation(selectedPatientId)
                } else {
                    Log.d("PatientLocation", "selectedPatient este null sau gol.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("PatientLocation", "Eroare la obținerea utilizatorului curent", e)
            }
    }

    private fun getAddressFromLocation(latLng: LatLng): String? {
        return try {
            val geocoder = android.location.Geocoder(requireContext())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                address.getAddressLine(0)  // Adresa completă
            } else null
        } catch (e: Exception) {
            Log.e("Geocoder", "Eroare la obținerea adresei", e)
            null
        }
    }

//    private fun loadPatientLocation(patientId: String) {
//        val db = FirebaseFirestore.getInstance()
//
//        db.collection("users").document(patientId).get()
//            .addOnSuccessListener { snapshot ->
//                val patient = snapshot.toObject(User::class.java)
//                val location = patient?.location
//
//                if (location != null) {
//                    val latLng = LatLng(location.latitude, location.longitude)
//                    map.clear()
//                    map.addMarker(MarkerOptions().position(latLng).title("Locația pacientului"))
//                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
//                } else {
//                    Log.d("PatientLocation", "Pacientul nu are o locație salvată.")
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.e("PatientLocation", "Eroare la obținerea locației pacientului", e)
//            }
//    }

    private fun loadPatientLocation(patientId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(patientId).get()
            .addOnSuccessListener { snapshot ->
                val patient = snapshot.toObject(User::class.java)
                val location = patient?.location
                val photoUrl = patient?.profilePictureUrl

                if (location != null && !photoUrl.isNullOrEmpty()) {
                    val latLng = LatLng(location.latitude, location.longitude)

                    Glide.with(this)
                        .asBitmap()
                        .load(photoUrl)
                        .circleCrop()
                        .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                            override fun onResourceReady(
                                resource: android.graphics.Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?
                            ) {
                                val resizedBitmap = Bitmap.createScaledBitmap(resource, 115, 115, false)
                                val descriptor = com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(resizedBitmap)

                                map.clear()
                                map.addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title("Locația pacientului")
                                        .icon(descriptor)
                                )
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                            }

                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                        })
                } else {
                    Log.d("PatientLocation", "Pacientul nu are o locație sau imagine de profil.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("PatientLocation", "Eroare la obținerea locației pacientului", e)
            }
    }
}