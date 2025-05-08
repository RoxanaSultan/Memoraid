package com.example.memoraid.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
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

    private fun loadPatientLocation(patientId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(patientId).get()
            .addOnSuccessListener { snapshot ->
                val patient = snapshot.toObject(User::class.java)
                val location = patient?.location

                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.clear()
                    map.addMarker(MarkerOptions().position(latLng).title("Locația pacientului"))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                } else {
                    Log.d("PatientLocation", "Pacientul nu are o locație salvată.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("PatientLocation", "Eroare la obținerea locației pacientului", e)
            }
    }
}