package com.roxanasultan.memoraid.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class LocationPoint(
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val arrivalTime: Timestamp = Timestamp.now(),
    val duration: Long = 0
)