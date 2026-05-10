package com.example.namma_railu

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RailBuddyApp(viewModel: RailBuddyViewModel = viewModel()) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var hasLocationPermission by remember { mutableStateOf(context.hasRailLocationPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result.values.any { it }
        viewModel.setLiveLocationEnabled(hasLocationPermission)
    }

    val alarmTriggered = viewModel.alarmTriggered()
    LaunchedEffect(alarmTriggered) {
        if (alarmTriggered) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    RailLocationTracker(
        active = viewModel.useLiveLocation,
        hasPermission = hasLocationPermission,
    ) { sample ->
        viewModel.updateLiveLocation(sample)
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Namma-Railu Buddy", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroCard(viewModel = viewModel, alarmTriggered = alarmTriggered)
            LiveStationCard(
                viewModel = viewModel,
                hasLocationPermission = hasLocationPermission,
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                },
            )
            CoachLayoutCard(viewModel = viewModel)
            PlatformPingCard(viewModel = viewModel)
            DestinationAlarmCard(viewModel = viewModel)
        }
    }
}

@Composable
private fun HeroCard(
    viewModel: RailBuddyViewModel,
    alarmTriggered: Boolean,
) {
    val origin = viewModel.selectedOriginStation()
    val destination = viewModel.selectedDestinationStation()
    val routeDistanceKm = viewModel.routeDistanceMeters() / 1000f

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Passenger Guide for Local Trains",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Select a live station, check the coach position, confirm platform pings, and keep the 5 km wake-up alarm ready.",
                lineHeight = 22.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(text = origin.name)
                Pill(text = destination.name)
                Pill(text = if (alarmTriggered) "Alarm active" else "Alarm armed")
            }
            Text(
                text = "Route distance: ${formatDistance(routeDistanceKm)}",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LiveStationCard(
    viewModel: RailBuddyViewModel,
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    val origin = viewModel.selectedOriginStation()
    val destination = viewModel.selectedDestinationStation()
    val activeLocation = viewModel.activeLocation()
    val currentDistance = viewModel.distanceToDestinationKilometers()

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "Live Station", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Pick the boarding station and the destination for the alarm.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    StationMenu(
                        label = "Your station",
                        selected = origin.name,
                        options = viewModel.stations,
                        onSelected = viewModel::selectOriginStation,
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    StationMenu(
                        label = "Destination",
                        selected = destination.name,
                        options = viewModel.stations,
                        onSelected = viewModel::selectDestinationStation,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Use live GPS")
                    Text(
                        text = if (hasLocationPermission) {
                            "Permission granted - real location updates will be used."
                        } else {
                            "Grant location permission or stay in demo mode."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
                Switch(
                    checked = viewModel.useLiveLocation,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasLocationPermission) {
                            onRequestPermission()
                        } else {
                            viewModel.setLiveLocationEnabled(enabled)
                        }
                    }
                )
            }

            if (!viewModel.useLiveLocation) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Demo travel progress")
                        Text(text = viewModel.routeProgressLabel(), fontWeight = FontWeight.SemiBold)
                    }
                    Slider(
                        value = viewModel.demoProgress,
                        onValueChange = viewModel::updateDemoProgress,
                    )
                    Text(
                        text = "Move the slider to simulate the train approaching the destination. The alarm will automatically trigger when the calculated distance falls below 5 km.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }

            OutlinedCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Location source", fontWeight = FontWeight.Bold)
                    Text(text = viewModel.currentLocationLabel())
                    Text(
                        text = "Active position: ${formatCoordinate(activeLocation.latitude)}, ${formatCoordinate(activeLocation.longitude)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "Distance to ${destination.name}: ${formatDistance(currentDistance)}",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Text(
                text = origin.routeNote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CoachLayoutCard(viewModel: RailBuddyViewModel) {
    val station = viewModel.selectedOriginStation()

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "Coach Layout", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = station.coachNote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Badge {
                Text(text = station.coachPosition.label)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                viewModel.coachSegments.forEachIndexed { index, segment ->
                    val highlighted = index == 1
                    CoachSegmentRow(
                        segment = segment,
                        highlighted = highlighted,
                    )
                }
            }

            Text(
                text = station.crowdNote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun CoachSegmentRow(
    segment: CoachSegment,
    highlighted: Boolean,
) {
    val accent = if (highlighted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (highlighted) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (highlighted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .background(accent, shape = MaterialTheme.shapes.medium)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = segment.title, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = segment.subtitle, color = textColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PlatformPingCard(viewModel: RailBuddyViewModel) {
    val pings = viewModel.visiblePings()
    val selectedStation = viewModel.selectedOriginStation()

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "Platform Ping", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Users can confirm the platform and add a short crowd update. The confirmation count stays visible for everyone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = viewModel.pingSyncStatus,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Post a ping for ${selectedStation.name}", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Platform ${viewModel.draftPlatformNumber}", fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = viewModel.draftPlatformNumber.toFloat(),
                            onValueChange = { viewModel.updateDraftPlatformNumber(it.roundToInt()) },
                            valueRange = 1f..selectedStation.platformCount.coerceAtLeast(2).toFloat(),
                            steps = (selectedStation.platformCount.coerceAtLeast(2) - 2).coerceAtLeast(0),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = viewModel.draftMessage,
                        onValueChange = viewModel::updateDraftMessage,
                        label = { Text("Crowd note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Button(
                        onClick = viewModel::postPing,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "Publish ping")
                    }
                }
            }

            if (pings.isEmpty()) {
                Text(text = "No platform pings yet for this station.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pings.forEach { ping ->
                        OutlinedCard {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(text = "Platform ${ping.platformNumber}", fontWeight = FontWeight.Bold)
                                    Badge { Text(text = "${ping.confirmations} confirmed") }
                                }
                                Text(text = ping.message)
                                Text(
                                    text = "Updated ${formatTime(ping.updatedAtMillis)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(
                                    onClick = { viewModel.confirmPing(ping.id) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(text = "Confirm")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationAlarmCard(viewModel: RailBuddyViewModel) {
    val destination = viewModel.selectedDestinationStation()
    val distanceKm = viewModel.distanceToDestinationKilometers()
    val isTriggered = viewModel.alarmTriggered()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTriggered) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isTriggered) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Destination Alarm", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "Wake-up target: ${destination.name}")
            Text(text = viewModel.alarmStatusText(), fontWeight = FontWeight.SemiBold)
            Text(
                text = "Current distance: ${formatDistance(distanceKm)} • Threshold: 5.0 km",
                color = if (isTriggered) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Arm alarm")
                    Text(
                        text = "Turn it off if you do not need the wake-up alert.",
                        fontSize = 13.sp,
                        color = if (isTriggered) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = viewModel.alarmArmed,
                    onCheckedChange = viewModel::updateAlarmArmed,
                )
            }
            if (isTriggered) {
                Text(
                    text = "Alarm triggered - you are inside the 5 km zone.",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun StationMenu(
    label: String,
    selected: String,
    options: List<RailStation>,
    onSelected: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = selected, modifier = Modifier.weight(1f))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { station ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = station.name)
                            Text(
                                text = station.district,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onSelected(station.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RailLocationTracker(
    active: Boolean,
    hasPermission: Boolean,
    onLocation: (LocationSample) -> Unit,
) {
    val context = LocalContext.current
    androidx.compose.runtime.DisposableEffect(active, hasPermission) {
        if (!active || !hasPermission) {
            onDispose { }
        } else {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
                .setMinUpdateIntervalMillis(2_000L)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    onLocation(
                        LocationSample(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            source = "Live GPS",
                            accuracyMeters = location.accuracy,
                            updatedAtMillis = System.currentTimeMillis(),
                        )
                    )
                }
            }

            try {
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            } catch (_: SecurityException) {
                // If permission changes between the UI check and the request, fall back to demo mode.
            }

            onDispose {
                client.removeLocationUpdates(callback)
            }
        }
    }
}

private fun Context.hasRailLocationPermissions(): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

private fun formatDistance(distanceKm: Float): String {
    return if (distanceKm >= 1f) {
        String.format(Locale.getDefault(), "%.1f km", distanceKm)
    } else {
        "${(distanceKm * 1000).roundToInt()} m"
    }
}

private fun formatCoordinate(value: Double): String {
    return String.format(Locale.getDefault(), "%.5f", value)
}

private fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}


