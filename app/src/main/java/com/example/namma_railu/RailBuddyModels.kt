package com.example.namma_railu

import kotlin.math.abs

data class RailStation(
    val id: String,
    val name: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
    val platformCount: Int,
    val coachPosition: CoachPosition,
    val coachNote: String,
    val crowdNote: String,
    val routeNote: String,
)

data class CoachSegment(
    val title: String,
    val subtitle: String,
)

data class PlatformPing(
    val id: Long,
    val stationId: String,
    val platformNumber: Int,
    val message: String,
    val confirmations: Int,
    val updatedAtMillis: Long,
)

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val source: String,
    val accuracyMeters: Float? = null,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

enum class CoachPosition(val label: String) {
    FRONT("Front / Engine side"),
    MIDDLE("Middle coaches"),
    REAR("Rear / Guard side"),
}

object RailBuddyCatalog {
    val stations: List<RailStation> = listOf(
        RailStation(
            id = "mandya",
            name = "Mandya",
            district = "Mysuru division",
            latitude = 12.5216,
            longitude = 76.8959,
            platformCount = 3,
            coachPosition = CoachPosition.FRONT,
            coachNote = "General coach is at the front (engine side).",
            crowdNote = "Popular with sugar factory workers and early city commuters.",
            routeNote = "Quick access to city-bound MEMU and passenger services."
        ),
        RailStation(
            id = "birur",
            name = "Birur",
            district = "Chikkamagaluru district",
            latitude = 13.7628,
            longitude = 75.9711,
            platformCount = 2,
            coachPosition = CoachPosition.MIDDLE,
            coachNote = "General coach stays near the middle for smoother boarding.",
            crowdNote = "Useful for students and office commuters changing trains.",
            routeNote = "Good for cross-line connections and long-distance transfers."
        ),
        RailStation(
            id = "mysuru",
            name = "Mysuru Junction",
            district = "Mysuru city",
            latitude = 12.2958,
            longitude = 76.6394,
            platformCount = 6,
            coachPosition = CoachPosition.FRONT,
            coachNote = "General coach is at the front near the engine side.",
            crowdNote = "Busy during festival and market hours.",
            routeNote = "Central hub for passengers moving between town and suburbs."
        ),
        RailStation(
            id = "tumakuru",
            name = "Tumakuru",
            district = "Tumakuru district",
            latitude = 13.3419,
            longitude = 77.1010,
            platformCount = 4,
            coachPosition = CoachPosition.REAR,
            coachNote = "General coach is often at the rear, closest to the guard side.",
            crowdNote = "Crowds peak around college and industrial shift timings.",
            routeNote = "Useful for daily workers heading toward the industrial belt."
        ),
        RailStation(
            id = "hassan",
            name = "Hassan",
            district = "Hassan district",
            latitude = 13.0068,
            longitude = 76.0968,
            platformCount = 3,
            coachPosition = CoachPosition.MIDDLE,
            coachNote = "General coach sits close to the middle for easier reach.",
            crowdNote = "First-time travelers often need a clear platform indicator here.",
            routeNote = "A reliable stop for rural-to-city passengers and interchanges."
        ),
        RailStation(
            id = "bengaluru",
            name = "Bengaluru City",
            district = "Bengaluru urban",
            latitude = 12.9776,
            longitude = 77.5666,
            platformCount = 10,
            coachPosition = CoachPosition.REAR,
            coachNote = "General coach is often near the rear so passengers can exit quickly.",
            crowdNote = "Very high footfall; platform announcements can be hard to hear.",
            routeNote = "Large interchange for onward city travel and local commuters."
        ),
    )

    val coachSegments: List<CoachSegment> = listOf(
        CoachSegment("Engine side", "Train front and locomotive end"),
        CoachSegment("General Coach", "The coach highlighted for quick boarding guidance"),
        CoachSegment("Ladies Coach", "Reserved seating for women passengers"),
        CoachSegment("Reserved Coach", "Season-ticket and reserved seating zone"),
        CoachSegment("Luggage / Parcel", "Space for parcels and larger baggage"),
        CoachSegment("Guard side", "Rear safety and brake van end"),
    )

    val initialPings: Map<String, List<PlatformPing>> = mapOf(
        "mandya" to listOf(
            PlatformPing(
                id = 1L,
                stationId = "mandya",
                platformNumber = 2,
                message = "Local passenger train likely on Platform 2 near the footbridge.",
                confirmations = 14,
                updatedAtMillis = System.currentTimeMillis() - 9 * 60_000L,
            ),
            PlatformPing(
                id = 2L,
                stationId = "mandya",
                platformNumber = 3,
                message = "Crowd is lighter on Platform 3; watch the timetable board.",
                confirmations = 7,
                updatedAtMillis = System.currentTimeMillis() - 22 * 60_000L,
            )
        ),
        "birur" to listOf(
            PlatformPing(
                id = 3L,
                stationId = "birur",
                platformNumber = 1,
                message = "Incoming passenger service confirmed on Platform 1.",
                confirmations = 11,
                updatedAtMillis = System.currentTimeMillis() - 14 * 60_000L,
            )
        ),
        "mysuru" to listOf(
            PlatformPing(
                id = 4L,
                stationId = "mysuru",
                platformNumber = 4,
                message = "Most commuters reported Platform 4 for the next MEMU arrival.",
                confirmations = 18,
                updatedAtMillis = System.currentTimeMillis() - 6 * 60_000L,
            ),
            PlatformPing(
                id = 5L,
                stationId = "mysuru",
                platformNumber = 5,
                message = "Backup update: Platform 5 may be used for a delayed service.",
                confirmations = 9,
                updatedAtMillis = System.currentTimeMillis() - 19 * 60_000L,
            )
        ),
        "tumakuru" to listOf(
            PlatformPing(
                id = 6L,
                stationId = "tumakuru",
                platformNumber = 2,
                message = "Down passenger service confirmed at Platform 2.",
                confirmations = 12,
                updatedAtMillis = System.currentTimeMillis() - 11 * 60_000L,
            )
        ),
        "hassan" to listOf(
            PlatformPing(
                id = 7L,
                stationId = "hassan",
                platformNumber = 1,
                message = "Board from Platform 1; station volunteers verified it.",
                confirmations = 10,
                updatedAtMillis = System.currentTimeMillis() - 16 * 60_000L,
            )
        ),
        "bengaluru" to listOf(
            PlatformPing(
                id = 8L,
                stationId = "bengaluru",
                platformNumber = 6,
                message = "City-side local service announced for Platform 6.",
                confirmations = 24,
                updatedAtMillis = System.currentTimeMillis() - 5 * 60_000L,
            )
        ),
    )

    fun stationById(id: String): RailStation = stations.first { it.id == id }
}

