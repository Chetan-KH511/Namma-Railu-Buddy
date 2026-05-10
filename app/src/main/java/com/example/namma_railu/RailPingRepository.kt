package com.example.namma_railu

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class RailPingRepository(context: Context) {
    private val localCache: MutableMap<String, List<PlatformPing>> =
        RailBuddyCatalog.initialPings.mapValues { (_, pings) -> pings.toList() }.toMutableMap()
    private val seededStations = mutableSetOf<String>()
    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val firestore: FirebaseFirestore? = FirebaseApp.initializeApp(context)?.let {
        FirebaseFirestore.getInstance(it)
    }

    val connectionLabel: String = if (firestore == null) {
        "Firebase not configured — using local demo store"
    } else {
        "Firebase ready — syncing platform pings"
    }

    fun observeStation(
        stationId: String,
        onPingsChanged: (List<PlatformPing>) -> Unit,
        onStatusChanged: (String) -> Unit,
    ) {
        onPingsChanged(localCache[stationId].orEmpty())

        val db = firestore ?: run {
            onStatusChanged(connectionLabel)
            return
        }

        listeners.values.forEach { it.remove() }
        listeners.clear()
        onStatusChanged("Firebase connected — loading ${stationName(stationId)} pings")

        listeners[stationId] = db
            .collection(stationCollectionPath(stationId))
            .orderBy("updatedAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onStatusChanged("Firebase sync error — ${error.message ?: "unknown error"}")
                    onPingsChanged(localCache[stationId].orEmpty())
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val remotePings = snapshot.documents.mapNotNull { doc ->
                    doc.toPlatformPing(stationId)
                }

                if (remotePings.isEmpty()) {
                    val seeded = localCache[stationId].orEmpty()
                    if (seeded.isNotEmpty() && seededStations.add(stationId)) {
                        seedStation(db, stationId, seeded)
                        onStatusChanged("Firebase connected — seeding demo pings for ${stationName(stationId)}")
                    } else {
                        onPingsChanged(seeded)
                        onStatusChanged("Firebase connected — no remote pings yet for ${stationName(stationId)}")
                    }
                    return@addSnapshotListener
                }

                localCache[stationId] = remotePings
                onPingsChanged(remotePings)
                onStatusChanged("Firebase synced — ${remotePings.size} live pings for ${stationName(stationId)}")
            }
    }

    fun postPing(
        stationId: String,
        platformNumber: Int,
        message: String,
        onStatusChanged: (String) -> Unit,
    ): List<PlatformPing> {
        val sanitizedMessage = message.trim().ifBlank {
            "Platform $platformNumber confirmed by a local commuter"
        }
        val ping = PlatformPing(
            id = System.currentTimeMillis(),
            stationId = stationId,
            platformNumber = platformNumber,
            message = sanitizedMessage,
            confirmations = 1,
            updatedAtMillis = System.currentTimeMillis(),
        )
        val updated = listOf(ping) + localCache[stationId].orEmpty()
        localCache[stationId] = updated
        syncPingToFirestore(stationId, ping, onStatusChanged)
        return updated
    }

    fun confirmPing(
        stationId: String,
        pingId: Long,
        onStatusChanged: (String) -> Unit,
    ): List<PlatformPing> {
        val updated = localCache[stationId].orEmpty().map { ping ->
            if (ping.id == pingId) {
                ping.copy(
                    confirmations = ping.confirmations + 1,
                    updatedAtMillis = System.currentTimeMillis(),
                )
            } else {
                ping
            }
        }
        localCache[stationId] = updated
        syncConfirmationToFirestore(stationId, pingId, onStatusChanged)
        return updated
    }

    fun close() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }

    private fun seedStation(db: FirebaseFirestore, stationId: String, pings: List<PlatformPing>) {
        if (pings.isEmpty()) return

        val batch = db.batch()
        pings.forEach { ping ->
            batch.set(
                db.collection(stationCollectionPath(stationId)).document(ping.id.toString()),
                ping.toFirestorePayload(),
            )
        }
        batch.commit()
    }

    private fun syncPingToFirestore(
        stationId: String,
        ping: PlatformPing,
        onStatusChanged: (String) -> Unit,
    ) {
        val db = firestore ?: run {
            onStatusChanged("Local demo store updated — Firebase unavailable")
            return
        }

        db.collection(stationCollectionPath(stationId))
            .document(ping.id.toString())
            .set(ping.toFirestorePayload())
            .addOnSuccessListener {
                onStatusChanged("Firebase synced — new ping posted for ${stationName(stationId)}")
            }
            .addOnFailureListener { error ->
                onStatusChanged("Firebase post failed — ${error.message ?: "unknown error"}")
            }
    }

    private fun syncConfirmationToFirestore(
        stationId: String,
        pingId: Long,
        onStatusChanged: (String) -> Unit,
    ) {
        val db = firestore ?: run {
            onStatusChanged("Local demo store updated — Firebase unavailable")
            return
        }

        db.collection(stationCollectionPath(stationId))
            .document(pingId.toString())
            .update(
                mapOf(
                    "confirmations" to FieldValue.increment(1),
                    "updatedAtMillis" to System.currentTimeMillis(),
                )
            )
            .addOnSuccessListener {
                onStatusChanged("Firebase synced — confirmation count updated for ${stationName(stationId)}")
            }
            .addOnFailureListener { error ->
                onStatusChanged("Firebase confirmation failed — ${error.message ?: "unknown error"}")
            }
    }

    private fun stationCollectionPath(stationId: String): String =
        "rail_stations/$stationId/platform_pings"

    private fun stationName(stationId: String): String =
        RailBuddyCatalog.stations.firstOrNull { it.id == stationId }?.name ?: stationId

    private fun com.google.firebase.firestore.DocumentSnapshot.toPlatformPing(stationId: String): PlatformPing? {
        val id = getLong("id") ?: id.toLongOrNull() ?: System.currentTimeMillis()
        val message = getString("message")?.trim().orEmpty()
        if (message.isBlank()) return null

        return PlatformPing(
            id = id,
            stationId = getString("stationId") ?: stationId,
            platformNumber = (getLong("platformNumber") ?: 1L).toInt(),
            message = message,
            confirmations = (getLong("confirmations") ?: 0L).toInt(),
            updatedAtMillis = getLong("updatedAtMillis") ?: System.currentTimeMillis(),
        )
    }

    private fun PlatformPing.toFirestorePayload(): Map<String, Any> = mapOf(
        "id" to id,
        "stationId" to stationId,
        "platformNumber" to platformNumber,
        "message" to message,
        "confirmations" to confirmations,
        "updatedAtMillis" to updatedAtMillis,
    )
}

