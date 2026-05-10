# Namma-Railu Buddy

Namma-Railu Buddy is a high-contrast Android Compose prototype for local train passengers.
It focuses on the core infrastructure goals from the prompt:

- **Live Station** selector for boarding and destination stations
- **Coach Layout** visualization with a highlighted general-coach position
- **Platform Ping** crowdsourcing with visible confirmation counts
- **Destination Alarm** that triggers when the current location is within **5 km** of the selected destination

## What is implemented

- Jetpack Compose UI with a high-contrast rail-focused theme
- Demo route mode for testing without GPS
- Live GPS mode using fused location updates when permission is granted
 - Platform ping posting and confirmation counts stored locally, with optional Firebase Firestore sync (the app now signs in anonymously by default when Firebase is present)
- Distance calculations based on station coordinates

## Run it

```powershell
Set-Location "C:\Users\legen\AndroidStudioProjects\Namma_Railu"
.\gradlew.bat :app:assembleDebug
```

## Notes

- The current version uses a **local in-app crowdsourcing model** so the project works out of the box.
- If Firebase is configured, `RailPingRepository.kt` automatically syncs platform pings to Firestore and falls back to the local store if the app has no `google-services.json` yet.
- To enable full cloud sync in your own Firebase project, add the Firebase Android config file at `app/google-services.json` and keep the Firestore collection path as `rail_stations/{stationId}/platform_pings`.
- The alarm is designed to be testable in the app itself through the demo progress slider, which is useful when GPS is unavailable.

## Main files

- `app/src/main/java/com/example/namma_railu/MainActivity.kt`
- `app/src/main/java/com/example/namma_railu/RailBuddyScreen.kt`
- `app/src/main/java/com/example/namma_railu/RailBuddyViewModel.kt`
- `app/src/main/java/com/example/namma_railu/RailBuddyModels.kt`

