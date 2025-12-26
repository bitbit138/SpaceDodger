# Rising Lion II
**Android Game Development Assignment - Phase 2**

## Overview
Rising Lion II is an arcade space-shooter where the player controls a fighter jet to stop a nuclear threat. This Phase 2 update transforms the project into a sensor-driven experience demonstrating Android Fragments, Google Maps API, Location Services, and Data Persistence.

## Phase 2 Updates
* **Advanced Controls:** Implemented SensorManager for tilt controls. The X-axis controls lateral movement, while the Y-axis controls game speed. Added a toggle for Button Mode for testing.
* **Fragments & UI:** Rebuilt the High Score screen using a split-view architecture containing a ListFragment and a MapFragment. Clicking a score communicates via an Interface to zoom the map to the recorded location.
* **Location & Maps:** Integrated FusedLocationProviderClient to capture GPS coordinates upon achieving a high score and the Google Maps SDK to visualize markers.
* **Gameplay:** Expanded grid to 5 lanes, added ammo collectibles (+50 points), and implemented vibration and sound effects.

## Key Features
* **Dynamic Physics:** Game speed adjusts in real-time based on device tilt.
* **Persistence:** High scores and location data are saved locally using SharedPreferences.
* **Feedback:** Dynamic UI updates, toast messages, haptic feedback (vibration), and audio effects.

## Technical Stack
* **Language:** Kotlin
* **Minimum SDK:** 24
* **Architecture:** MVC with Activities and Fragments.
* **APIs:** Google Maps SDK, Android Location Services, Android Sensors.

## How to Run
1. Obtain a Google Maps API Key and paste it into the meta-data tag in `app/src/main/AndroidManifest.xml`.
2. Grant Location permissions when prompted on the first launch.
3. Run on a physical Android device to ensure proper accelerometer functionality.