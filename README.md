# Namma Railu Buddy

Namma Railu Buddy is an Android passenger guide for local train commuters. It helps users pick a station, view platform and coach-position guidance, crowd-confirm platform changes, share delay updates, and set a destination alarm that fires within a 5 km radius of the selected station.

## Requirement Coverage

- Live station selection with sample Karnataka stations such as Mandya and Birur.
- Train selection for each station, with sample local passenger/MEMU services.
- Coach-position display for General and Ladies coaches.
- Platform Ping backed by Firestore confirmation counts.
- Crowdsourced delay updates backed by Firestore.
- Firebase Authentication using anonymous sign-in.
- Destination alarm using Android geofencing and notifications.
- High-contrast, large-type UI designed for quick use in crowded railway stations.

## Firebase Setup

1. Create a Firebase project.
2. Add an Android app with package name `com.mindmatrix.nammarailubuddy`.
3. Download `google-services.json` and place it at `app/google-services.json`.
4. Enable Authentication > Sign-in method > Anonymous.
5. Create a Firestore database.

The app writes live data under:

```text
stations/{stationId}/platformPings/{trainId}
stations/{stationId}/delayUpdates/{trainId}
```

For development, start with permissive Firestore rules, then tighten them before release:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /stations/{stationId}/{collection}/{trainId} {
      allow read: if true;
      allow write: if request.auth != null
        && collection in ['platformPings', 'delayUpdates'];
    }
  }
}
```

## Build

Open the folder in Android Studio and let Gradle sync the project. Then run the `app` configuration on a device or emulator with Google Play services.

The project can sync before `app/google-services.json` is added. Firebase-backed platform pings and delay reports become active after that file is present and the project is rebuilt.

Location and notification permissions are requested at runtime. On Android 10 and later, background geofence behavior may require setting location access to "Allow all the time" from system settings for the most reliable destination alarms.
