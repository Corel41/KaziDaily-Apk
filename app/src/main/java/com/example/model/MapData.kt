package com.example.model

data class LocationCoordinate(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

data class RouteInfo(
    val originName: String,
    val destinationName: String,
    val distanceKm: Double,
    val walkingMinutes: Int,
    val drivingMinutes: Int,
    val bodaBodaMinutes: Int,
    val turnDirections: List<String>
)

object NairobiLocations {
    val CURRENT_USER_LOC = LocationCoordinate(
        name = "My Location (Westlands)",
        description = "Waiyaki Way, Westlands, Nairobi",
        latitude = -1.2673,
        longitude = 36.8055
    )

    val PRESET_LOCATIONS = listOf(
        CURRENT_USER_LOC,
        LocationCoordinate("Kilimani", "Argwings Kodhek Rd, Nairobi", -1.2891, 36.7865),
        LocationCoordinate("Nairobi CBD", "Moi Avenue / Kencom, Nairobi", -1.2864, 36.8249),
        LocationCoordinate("Upper Hill", "Hospital Rd, Nairobi", -1.2985, 36.8188),
        LocationCoordinate("Karen", "Ngong Rd / Karen Shopping Centre", -1.3204, 36.7058),
        LocationCoordinate("Kasarani", "Thika Superhighway, Kasarani", -1.2215, 36.8964),
        LocationCoordinate("Eastleigh", "1st Avenue, Eastleigh", -1.2778, 36.8523),
        LocationCoordinate("Lavington", "James Gichuru Rd, Lavington", -1.2785, 36.7725),
        LocationCoordinate("Parklands", "3rd Parklands Avenue, Nairobi", -1.2612, 36.8198),
        LocationCoordinate("South B", "Mombasa Rd / South B Estate", -1.3120, 36.8378)
    )

    fun calculateRoute(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double, destName: String): RouteInfo {
        // Approximate Haversine distance in km
        val earthRadius = 6371.0
        val dLat = Math.toRadians(toLat - fromLat)
        val dLng = Math.toRadians(toLng - fromLng)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = Math.max(0.6, Math.round(earthRadius * c * 10.0) / 10.0)

        val drivingMins = Math.max(5, (distance * 3.5).toInt())
        val bodaMins = Math.max(4, (distance * 2.2).toInt())
        val walkMins = Math.max(12, (distance * 13.0).toInt())

        val steps = listOf(
            "Start heading towards main corridor from current location (0.2 km)",
            "Turn onto connected avenue towards $destName ($distance km)",
            "Follow arterial road keeping right at the roundabout",
            "Arrive at destination: $destName (precise job site coordinates)"
        )

        return RouteInfo(
            originName = "Current Location",
            destinationName = destName,
            distanceKm = distance,
            walkingMinutes = walkMins,
            drivingMinutes = drivingMins,
            bodaBodaMinutes = bodaMins,
            turnDirections = steps
        )
    }
}
