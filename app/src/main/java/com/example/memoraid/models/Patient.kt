data class Patient(
    val id: String = "", // Matches the User ID
    val medicalHistory: String? = null,
    val assignedCaretakers: List<String> = listOf(), // List of caretaker IDs
    val location: Location? = null // Optional: Current location
)

data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)