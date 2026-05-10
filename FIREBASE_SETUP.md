# Firebase setup for Namma-Railu Buddy

This app is already coded to **fallback to a local demo store** when Firebase is not configured.
To turn on real Firestore syncing for `Platform Ping`, follow these steps.

## What you need to create in Firebase

1. **A Firebase project**
2. **An Android app registration** inside that Firebase project
3. **A Firestore database**
4. **A `google-services.json` file** downloaded from Firebase and placed in `app/`

You do **not** need to manually create the `rail_stations/.../platform_pings` collection path.
Firestore will create it the first time the app writes a ping.

---

## Step-by-step: connect the app to Firebase

### 1) Create a Firebase project
- Open the Firebase Console
- Click **Add project**
- Enter a project name, for example `Namma Railu Buddy`
- Continue through the setup flow
- Google Analytics is optional for this app

### 2) Register the Android app
Inside your Firebase project:
- Click the Android icon to add an app
- Use this package name:
  `com.example.namma_railu`
- Add an optional nickname if you want
- You can skip the SHA-1 for now unless you later add sign-in or dynamic links
- Register the app

### 3) Download `google-services.json`
- After registration, Firebase will offer a download button
- Download `google-services.json`
- Copy it into:
  `C:\Users\legen\AndroidStudioProjects\Namma_Railu\app\google-services.json`

### 4) Add the Google Services Gradle plugin
The project is currently left buildable without Firebase. To activate Firebase in a real setup, add the Google Services plugin in the app module.

In `app/build.gradle.kts`, apply:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // add Google Services plugin here
}
```

And in the root `build.gradle.kts`, keep the plugin available if you use the version catalog approach.

If you want, I can re-add this plugin once you have `google-services.json` in place.

### 5) Enable Firestore Database
In Firebase Console:
- Open **Build** → **Firestore Database**
- Click **Create database**
- Start in **production mode** if you plan to lock it down later, or **test mode** for quick prototyping
- Choose a location close to your users

### 6) Use this Firestore structure
The app writes platform pings under this path:

```text
rail_stations/{stationId}/platform_pings/{pingId}
```

Example documents:
- `rail_stations/mandya/platform_pings/1710000000000`
- `rail_stations/birur/platform_pings/1710000000100`

Each document contains:
- `id`
- `stationId`
- `platformNumber`
- `message`
- `confirmations`
- `updatedAtMillis`

### 7) Set Firestore rules
For development you can start with open rules, but a safer approach is to require authentication and allow only limited writes. The app now performs an **anonymous sign-in** automatically; use rules that require a signed-in user.

**Recommended (dev-to-stage) rules that require auth:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /rail_stations/{stationId}/platform_pings/{pingId} {
      // Allow read to everyone, but writes only for authenticated users.
      allow read: if true;
      allow create, update: if request.auth != null
        && request.resource.data.platformNumber is int
        && request.resource.data.platformNumber >= 1
        && request.resource.data.message is string
        && request.resource.data.message.size() <= 500;
    }
  }
}
```

This enforces:
- reads are public (so the app UI can show pings without signing in)
- writes require an authenticated user (the app signs in anonymously by default)
- `platformNumber` must be an integer >= 1
- `message` must be a string and limited to 500 characters

For stricter production rules, require a verified account and additional checks (rate limits, account age, etc.).

### 8) Rebuild the app
After adding `google-services.json` and the plugin, rebuild:

```powershell
Set-Location "C:\Users\legen\AndroidStudioProjects\Namma_Railu"
.\gradlew.bat :app:assembleDebug
```

---

## What the app will do after Firebase is connected

- Load station pings from Firestore
- Post new `Platform Ping` updates to Firestore
- Increment confirmation counts in Firestore
- Keep the local demo fallback if Firebase is unavailable

---

## Recommended Firebase extras later

If you want to expand this app beyond the prototype, consider:
- **Firebase Auth** for verified contributors
- **Firestore security rules** for anti-spam protection
- **Cloud Functions** to moderate or aggregate pings
- **Crashlytics** for production diagnostics

