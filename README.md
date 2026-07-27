# Aurora

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

Aurora turns an Android phone into the backend for a self-hosted bedside
smart display. It reads live data straight off the phone — battery,
notifications, calendar, alarms, weather — and a bedside sound machine you
control from your phone but that actually plays through the display's own
speakers, then serves all of it as JSON over your home Wi-Fi to
[**echo-dashboard**](https://github.com/HChaffin/echo-dashboard), a companion
kiosk frontend built for a repurposed Amazon Echo Show.

No cloud account, no subscription, no third-party server in the loop —
just your phone talking HTTP to a screen on the same network.

<p align="center">
  <img src="docs/screenshot-main.png" width="45%" alt="Aurora app main screen" />
  <img src="docs/screenshot-customize.png" width="45%" alt="Customize Dashboard screen" />
</p>

## Features

- **Live phone status**: battery level/charging state, notification count
  (grouped by app), today's calendar events, next alarm — all polled and
  served as one JSON snapshot.
- **Weather**: current conditions via [Open-Meteo](https://open-meteo.com/)
  (no API key needed), cached and refreshed in the background.
- **Bedside Sound Machine**: pick a built-in ambient sound or import your
  own (Storage Access Framework), control play/pause/stop/volume/sleep
  timer from either the phone or the dashboard — Aurora only tracks
  *desired* state, the dashboard's browser does the actual audio playback
  through the display's speakers.
- **Dashboard customization**: reorder, hide, or resize the dashboard's
  cards, right from the phone app — no code changes needed.
- **Zero cloud dependency**: everything is plain HTTP on your LAN.

## How it fits together

```
┌─────────────────┐        HTTP (LAN, port 8080)        ┌──────────────────┐
│  Aurora (phone)  │ ───────────────────────────────────▶│  echo-dashboard  │
│                  │◀─────────────────────────────────── │  (kiosk display) │
└─────────────────┘        polls every 30s               └──────────────────┘
```

Aurora runs an embedded HTTP server ([NanoHTTPD](https://github.com/NanoHttpd/nanohttpd))
on the phone. The dashboard — a static HTML/CSS/JS kiosk page, see
[echo-dashboard](https://github.com/HChaffin/echo-dashboard) — polls it every
30 seconds and renders the result. Both devices just need to be on the
same Wi-Fi network.

## Requirements

- An Android phone, API 26+ (Android 8.0), with a working Wi-Fi connection.
- [Android Studio](https://developer.android.com/studio) or a standalone
  JDK 17+ to build.
- A display to point [echo-dashboard](https://github.com/HChaffin/echo-dashboard)
  at — this was built for a rooted Amazon Echo Show 5 in kiosk mode, but
  any device with a browser on the same network works.

## Installation

1. **Clone and open the project:**

   ```
   git clone https://github.com/HChaffin/Aurora.git
   cd Aurora
   ```

   Open it in Android Studio, or build straight from the command line:

   ```
   ./gradlew assembleDebug
   ```

   The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

2. **Install it on your phone** (via Android Studio's Run button, or):

   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Launch Aurora.** The server starts automatically and the app shows
   its LAN address (e.g. `192.168.1.130:8080`) — tap **Copy Dashboard URL**
   to grab it.

4. **Grant permissions** when prompted:
   - **Notification access** (Settings deep link from the app) — needed
     for the notification count/grouping.
   - **Calendar access** (standard runtime dialog) — needed for today's
     events. Both are optional; declining just means those fields read
     empty instead of the app crashing.

5. **Point [echo-dashboard](https://github.com/HChaffin/echo-dashboard) at
   Aurora's address** — see that repo's README for setup.

## Configuration

Weather location is a hardcoded coordinate in `weather/WeatherConfig.kt`
(Aurora is a bedside dashboard bolted to one location, not a mobile app —
there's no GPS permission or location picker). Update it to your own
coordinates before building.

## API

`GET /health` → `Aurora OK`

`GET /dashboard` → one JSON snapshot of everything the display needs:

```json
{
  "battery": 82,
  "charging": true,
  "notifications": 5,
  "notificationGroups": [{ "app": "Messages", "count": 3 }],
  "nextAlarm": { "time": "07:00", "enabled": true },
  "calendar": [{ "title": "School", "start": "08:00", "end": "15:00", "allDay": false }],
  "weather": { "temperature": 74, "condition": "Clear", "high": 86, "low": 68 },
  "soundMachine": { "playing": false, "sound": null, "volume": 50, "sleepTimerMinutes": null },
  "layout": [{ "id": "weather", "visible": true, "size": "medium" }]
}
```

Absent values are always an explicit JSON `null` (or `[]`/`{}`), never an
omitted key — the response shape never changes based on what data happens
to be available.

Sound Machine control routes (`POST /sound/play`, `/sound/pause`,
`/sound/stop`, `/sound/volume`, `/sound/timer`, `GET /sound/library`,
`GET /sound/stream`) exist for the dashboard's own use — see
[echo-dashboard](https://github.com/HChaffin/echo-dashboard).

## Architecture

```
com.rusty.aurora
├── api/            HTTP layer: NanoHTTPD server + one Route per endpoint
├── alarm/          AlarmRepository - wraps AlarmManager.getNextAlarmClock()
├── battery/        BatteryRepository - wraps android.os.BatteryManager
├── calendar/       CalendarRepository - wraps the platform Calendar Provider
├── layout/         Dashboard tile order/visibility/size, persisted + served
├── notifications/  NotificationCountRepository + the NotificationListenerService
├── sound/          Sound Machine state, library, sleep timer (Aurora never plays audio itself)
├── weather/        WeatherRepository - cached Open-Meteo client
├── di/             Hand-rolled composition root (AppContainer)
├── model/          Serializable response DTOs, shared enums
├── service/        AuroraServerController - server lifecycle, Activity-agnostic
├── ui/             Compose screens, ViewModel, MainActivity, theme
└── util/           NetworkUtil (LAN IP), NotificationAccessUtil (Settings deep link)
```

**Design principles this codebase follows throughout:**

- `api/` never contains business logic — `AuroraHttpServer` just matches
  a request against a `List<Route>` and delegates; each `Route` owns
  exactly one endpoint.
- Every data source is a repository *interface* (`BatteryRepository`,
  `WeatherRepository`, `SoundRepository`, ...) — callers depend on the
  interface, never the concrete Android/network type, which is what makes
  them fake-able in tests.
- Pure logic is split out from Android/network glue wherever a repository
  does real transformation (`CalendarEventMapper`, `WeatherCache`,
  `SleepTimerCalculator`, `NotificationGrouper`, ...) — these have no
  Android dependency and are covered by plain JVM unit tests.
- `di/AppContainer` is a hand-rolled object graph, not Hilt/Koin — still
  comfortably manageable by hand at this size, and every class still takes
  its dependencies through its constructor.
- Aurora never plays audio itself — the Sound Machine tracks desired state
  and serves raw bytes; the dashboard's browser does actual playback
  through the display's speakers, so bedside audio survives Aurora's
  process being killed by Android without a foreground service.

## Building & testing

Toolchain: AGP 9.3.0, Kotlin 2.2.10, Gradle 9.5.0, compileSdk/targetSdk 36,
minSdk 26.

```
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # all unit tests (plain JVM, no device needed)
./gradlew assembleRelease      # release variant, lint included
```

All tests are plain JVM unit tests under `app/src/test` — no emulator or
Robolectric required. They cover the HTTP layer end-to-end (a real
`AuroraHttpServer` on an ephemeral port, hit over real HTTP) and every pure
transformation function (calendar mapping, weather caching/parsing, sleep
timer math, notification grouping). Thin Android-integration edges
(`CalendarRepositoryImpl`, `AlarmRepositoryImpl`, `WeatherRepositoryImpl`,
`SoundRepositoryImpl`) are intentionally left to manual device
verification instead, since they hold no real logic of their own.

## Roadmap

Ideas not yet built, following the same repository-interface +
`DashboardResponse`-field pattern established so far: Wi-Fi status,
charging time estimate, more built-in sound machine tracks.

## License

[MIT](LICENSE)
