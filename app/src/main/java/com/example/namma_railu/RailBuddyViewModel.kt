package com.example.namma_railu

import android.app.Application
import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlin.math.max
import kotlin.math.roundToInt
import java.util.Locale

class RailBuddyViewModel(application: Application) : AndroidViewModel(application) {
    val stations: List<RailStation> = RailBuddyCatalog.stations
    val coachSegments: List<CoachSegment> = RailBuddyCatalog.coachSegments
    private val pingRepository = RailPingRepository(application.applicationContext)

    var selectedOriginStationId by mutableStateOf(stations.first().id)
        private set

    var selectedDestinationStationId by mutableStateOf(stations[2].id)
        private set

    var useLiveLocation by mutableStateOf(false)
        private set

    var liveLocation by mutableStateOf<LocationSample?>(null)
        private set

    var demoProgress by mutableFloatStateOf(0.62f)
        private set

    var alarmArmed by mutableStateOf(true)
        private set

    var draftPlatformNumber by mutableIntStateOf(2)
        private set

    var draftMessage by mutableStateOf("Platform 2 confirmed near the footbridge")
        private set

    var platformPings by mutableStateOf(RailBuddyCatalog.initialPings)
        private set

    var pingSyncStatus by mutableStateOf(pingRepository.connectionLabel)
        private set

    init {
        // Attempt anonymous Firebase sign-in so Firestore writes are permitted by rules that require auth.
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                pingSyncStatus = "Firebase signed in"
                observeSelectedStation(selectedOriginStationId)
            } else {
                pingSyncStatus = "Signing in anonymously to Firebase..."
                auth.signInAnonymously().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        pingSyncStatus = "Firebase anonymous sign-in succeeded"
                    } else {
                        pingSyncStatus = "Firebase anonymous sign-in failed — continuing with local demo store"
                    }
                    // Observe station regardless of sign-in success; repository will use local fallback if needed.
                    observeSelectedStation(selectedOriginStationId)
                }
            }
        } catch (e: Exception) {
            // If Firebase SDK or initialization is not present, fall back to local store.
            pingSyncStatus = "Firebase SDK not available — using local demo store"
            observeSelectedStation(selectedOriginStationId)
        }
    }

    fun selectOriginStation(stationId: String) {
        selectedOriginStationId = stationId
        observeSelectedStation(stationId)
    }

    fun selectDestinationStation(stationId: String) {
        selectedDestinationStationId = stationId
    }

    fun setLiveLocationEnabled(enabled: Boolean) {
        useLiveLocation = enabled
    }

    fun updateLiveLocation(sample: LocationSample) {
        liveLocation = sample
    }

    fun updateDemoProgress(value: Float) {
        demoProgress = value.coerceIn(0f, 1f)
    }

    fun updateAlarmArmed(armed: Boolean) {
        alarmArmed = armed
    }

    fun updateDraftPlatformNumber(value: Int) {
        draftPlatformNumber = value.coerceIn(1, 12)
    }

    fun updateDraftMessage(value: String) {
        draftMessage = value
    }

    fun resetDemoForTesting() {
        updateDemoProgress(0.62f)
        alarmArmed = true
    }

    fun postPing() {
        val stationId = selectedOriginStationId
        val sanitizedMessage = draftMessage.trim().ifBlank {
            "Platform $draftPlatformNumber confirmed by a local commuter"
        }
        val updatedPings = pingRepository.postPing(
            stationId = stationId,
            platformNumber = draftPlatformNumber,
            message = sanitizedMessage,
            onStatusChanged = { pingSyncStatus = it },
        )
        updateStationPings(stationId, updatedPings)
        draftMessage = ""
        draftPlatformNumber = max(1, draftPlatformNumber)
    }

    fun confirmPing(pingId: Long) {
        val stationId = selectedOriginStationId
        val updatedPings = pingRepository.confirmPing(
            stationId = stationId,
            pingId = pingId,
            onStatusChanged = { pingSyncStatus = it },
        )
        updateStationPings(stationId, updatedPings)
    }

    fun selectedOriginStation(): RailStation = RailBuddyCatalog.stationById(selectedOriginStationId)

    fun selectedDestinationStation(): RailStation = RailBuddyCatalog.stationById(selectedDestinationStationId)

    fun visiblePings(): List<PlatformPing> = platformPings[selectedOriginStationId].orEmpty()

    fun activeLocation(): LocationSample {
        val live = liveLocation
        if (useLiveLocation && live != null) {
            return live
        }
        return demoLocation()
    }

    fun routeDistanceMeters(): Float {
        val origin = selectedOriginStation()
        val destination = selectedDestinationStation()
        return distanceBetweenMeters(
            origin.latitude,
            origin.longitude,
            destination.latitude,
            destination.longitude,
        )
    }

    fun distanceToDestinationMeters(): Float {
        val current = activeLocation()
        val destination = selectedDestinationStation()
        return distanceBetweenMeters(
            current.latitude,
            current.longitude,
            destination.latitude,
            destination.longitude,
        )
    }

    fun distanceToDestinationKilometers(): Float = distanceToDestinationMeters() / 1000f

    fun alarmTriggered(): Boolean {
        return alarmArmed && distanceToDestinationMeters() <= 5_000f
    }

    fun alarmStatusText(): String {
        return if (alarmTriggered()) {
            "Wake-up alarm should fire now. You are within 5 km of ${selectedDestinationStation().name}."
        } else {
            val distanceKm = distanceToDestinationKilometers()
            "Alarm armed. Keep tracking ${selectedDestinationStation().name}; current distance is ${formatDistance(distanceKm)}."
        }
    }

    fun currentLocationLabel(): String {
        val live = liveLocation
        return when {
            useLiveLocation && live != null -> "Live GPS"
            useLiveLocation -> "Live GPS waiting for first fix"
            else -> "Demo route"
        }
    }

    fun routeProgressLabel(): String {
        val progressPercent = (demoProgress * 100).roundToInt()
        return "Demo progress $progressPercent%"
    }

    fun routeDescription(): String {
        val origin = selectedOriginStation()
        val destination = selectedDestinationStation()
        return if (origin.id == destination.id) {
            "Start and destination are the same station, so the alarm is instantly inside the 5 km radius."
        } else {
            "Route view: ${origin.name} → ${destination.name} (${formatDistance(routeDistanceMeters() / 1000f)})"
        }
    }

    private fun demoLocation(): LocationSample {
        val origin = selectedOriginStation()
        val destination = selectedDestinationStation()
        if (origin.id == destination.id) {
            return LocationSample(
                latitude = destination.latitude,
                longitude = destination.longitude,
                source = routeProgressLabel(),
            )
        }
        val progress = demoProgress.coerceIn(0f, 1f)
        val latitude = origin.latitude + (destination.latitude - origin.latitude) * progress
        val longitude = origin.longitude + (destination.longitude - origin.longitude) * progress
        return LocationSample(
            latitude = latitude,
            longitude = longitude,
            source = routeProgressLabel(),
        )
    }

    private fun observeSelectedStation(stationId: String) {
        pingRepository.observeStation(
            stationId = stationId,
            onPingsChanged = { pings -> updateStationPings(stationId, pings) },
            onStatusChanged = { status -> pingSyncStatus = status },
        )
    }

    private fun updateStationPings(stationId: String, pings: List<PlatformPing>) {
        platformPings = platformPings.toMutableMap().apply {
            put(stationId, pings)
        }
    }

    private fun distanceBetweenMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            startLatitude,
            startLongitude,
            endLatitude,
            endLongitude,
            results,
        )
        return results[0]
    }

    private fun formatDistance(distanceKm: Float): String {
        return if (distanceKm >= 1f) {
            "${String.format(Locale.getDefault(), "%.1f", distanceKm)} km"
        } else {
            "${(distanceKm * 1000).roundToInt()} m"
        }
    }

    override fun onCleared() {
        pingRepository.close()
        super.onCleared()
    }
}

