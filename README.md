# Aurora

Aurora is the "brains" behind a custom bedside dashboard: it runs on an
Android phone, reads live data off that phone (battery, notifications,
calendar, alarms, weather), and serves it as JSON over the local network to
a rooted Echo Show 5 running a kiosk-mode HTML/CSS/JS dashboard.

Phone and Echo Show are assumed to always be on the same Wi-Fi network;
Aurora talks HTTP, not cloud APIs, to the Echo Show. It does talk to one
external service - Open-Meteo, for weather - see below.

## Architecture

```
com.rusty.aurora
├── api/            HTTP layer: NanoHTTPD server + one Route per endpoint
├── alarm/          AlarmRepository - wraps AlarmManager.getNextAlarmClock()
├── battery/        BatteryRepository - wraps android.os.BatteryManager
├── calendar/       CalendarRepository - wraps the platform Calendar Provider
├── notifications/  NotificationCountRepository + the NotificationListenerService
├── weather/        WeatherRepository - cached Open-Meteo client
├── di/             Hand-rolled composition root (AppContainer)
├── model/          Serializable response DTOs, shared enums
├── service/        AuroraServerController - server lifecycle, Activity-agnostic
├── ui/             Compose screen, ViewModel, MainActivity, theme
└── util/           NetworkUtil (LAN IP), NotificationAccessUtil (Settings deep link)
```

### Why these boundaries

- **`api/` never contains business logic.** `AuroraHttpServer` only matches
  an incoming request against a `List<Route>` and delegates. Each `Route`
  (`HealthRoute`, `DashboardRoute`) owns exactly one endpoint. Everything
  new in v0.2 (calendar, alarm, weather) was added as new *fields* on the
  existing `/dashboard` response rather than new routes, per the v0.2 brief
  - `AuroraHttpServer` and the routing logic haven't changed since v0.1.

- **Repositories, not raw Android APIs, are what everything else depends
  on.** Every data source (`BatteryRepository`, `NotificationCountRepository`,
  `CalendarRepository`, `AlarmRepository`, `WeatherRepository`) is an
  interface; `DashboardRoute` and `AuroraViewModel` depend on the interface,
  never the concrete Android/network type. That's what makes them fake-able
  in tests and keeps Android framework types out of the HTTP and UI layers.

- **Pure logic is split out from Android/network glue wherever a
  repository does any real transformation.** `CalendarRepositoryImpl` and
  `WeatherRepositoryImpl` are thin and untested directly (like
  `BatteryRepositoryImpl` before them); the filtering/sorting/formatting
  logic behind them (`CalendarEventMapper`, `AlarmMapper`,
  `OpenMeteoResponseParser`, `WeatherConditionMapper`, `WeatherCache`) has
  no Android or network dependency and is covered by plain JVM unit tests.

- **`service/AuroraServerController` takes no Activity/Service context.**
  It's constructed with just a `(port: Int) -> AuroraHttpServer` factory.
  Today it's started from `AuroraViewModel`; moving the server into a
  foreground `Service` later (so it survives the app being backgrounded)
  is a change to *where* `start()`/`stop()` are called, not to this class.

- **`di/AppContainer` is a plain object graph, not Hilt/Koin.** Six
  repositories is still comfortably within "wire it by hand" territory - a
  DI framework would add build time and APK size for no benefit yet. Every
  class still takes its dependencies through its constructor, so the option
  to introduce one later (or swap in fakes for tests) stays open.

- **`AuroraNotificationListenerService` is the one sanctioned exception**
  to constructor injection. Android instantiates `NotificationListenerService`
  subclasses itself, so it can't receive dependencies through a
  constructor - it reads `(application as AuroraApplication).container`
  instead. It only recomputes a *count* from `activeNotifications`, never
  reads notification content.

- **Weather owns its own background scope.** `WeatherRepositoryImpl` is a
  singleton in `AppContainer` with no ViewModel-scoped lifecycle to borrow,
  so it holds its own `CoroutineScope(SupervisorJob() + Dispatchers.IO)`
  for refreshes - see [Weather caching](#weather-caching) below.

## API (v0.2)

`GET /health`
```
Aurora OK
```

`GET /dashboard`
```json
{
  "battery": 82,
  "charging": true,
  "notifications": 5,
  "nextAlarm": { "time": "07:00", "enabled": true },
  "calendar": [
    { "title": "School", "start": "08:00", "end": "15:00", "allDay": false }
  ],
  "weather": { "temperature": 74, "condition": "Clear", "high": 86, "low": 68 }
}
```

`battery`, `charging`, and `notifications` keep their v0.1 meaning and
position - existing clients reading only those three fields are
unaffected. New fields:

| Field | Type | When absent |
|---|---|---|
| `nextAlarm` | object or `null` | `null` if no alarm is scheduled |
| `calendar` | array (never `null`) | `[]` if no events today, or calendar permission was denied |
| `weather` | object or `null` | `null` only if Aurora has never fetched successfully (e.g. first run while offline) |

Absent values are always an explicit JSON `null` (or `[]` for calendar),
never an omitted key - `Json { encodeDefaults = true }` plus
kotlinx.serialization's default `explicitNulls = true` guarantee the shape
of the response never changes based on what data happens to be available.

Default port is `8080`, shown and copyable from the app UI.

## Permissions

| Permission | Kind | Why |
|---|---|---|
| `INTERNET` | normal | NanoHTTPD needs it to open a listening socket, and `WeatherRepository` needs it for the Open-Meteo request |
| `ACCESS_NETWORK_STATE` | normal | lets `WeatherRepository` skip a fetch attempt while offline instead of eating a timeout |
| `READ_CALENDAR` | **dangerous, runtime prompt** | reads today's events via the Calendar Provider |

Normal permissions are granted automatically at install time. `READ_CALENDAR`
is not - see below.

### Notification access

Not a runtime-permission dialog - a toggle in system Settings (Settings →
Notification access). The app detects when it's missing and shows a "Grant
Notification Access" button that deep links straight there via
`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`. Until granted,
`notifications` in `/dashboard` reads `0`.

### Calendar access

A standard Android runtime permission dialog, requested via
`ActivityResultContracts.RequestPermission()` in `MainActivity`. If denied:

- The app does not crash - `CalendarRepositoryImpl.getTodayEvents()` checks
  `hasCalendarPermission()` before ever touching the `ContentResolver`, so
  no `SecurityException` is possible.
- `/dashboard`'s `"calendar"` field simply reads `[]`, identical to "no
  events today."
- The UI shows a permanent "Grant Calendar Access" card (same pattern as
  the notification-access card) until it's granted.

## Weather caching

Aurora calls [Open-Meteo](https://open-meteo.com/) - no API key required -
for a fixed location (see `weather/WeatherConfig.kt`; currently Jacksonville,
FL 32258, approximate). Aurora is a bedside dashboard bolted to one physical
location, not a mobile app, so a hardcoded coordinate was chosen over adding
a location permission and taking a GPS fix on every refresh.

- **Cache window:** 15 minutes (`WeatherConfig.CACHE_DURATION_MILLIS`).
- **Never blocks `/dashboard`:** `WeatherRepository.getWeather()` always
  returns immediately - the current cached snapshot (possibly `null` on
  first run, possibly stale) - and separately kicks off a background
  refresh if the cache is missing or older than 15 minutes.
- **Refreshes asynchronously:** on `WeatherRepositoryImpl`'s own
  `CoroutineScope(SupervisorJob() + Dispatchers.IO)`, guarded by an
  `AtomicBoolean` so a burst of `/dashboard` polls while a refresh is
  already in flight never triggers duplicate network calls.
- **Offline handling:** `NetworkStatusProvider` checks connectivity before
  attempting a fetch; if offline, the refresh is skipped entirely rather
  than left to time out.
- **Failure handling:** a failed refresh (offline, timeout, malformed
  response) is caught and logged (`Log.w`), and never clears the existing
  cache - the last-known-good snapshot keeps being served. Only a location
  that has never fetched successfully reports `null`.

## Building

Toolchain: AGP 9.3.0, Kotlin 2.2.10, Gradle 9.5.0, compileSdk/targetSdk 36,
minSdk 26 - matched to whatever a fresh "New Project" in the installed
Android Studio itself generates, so `./gradlew` and Android Studio's own
sync/build should never disagree.

```
./gradlew assembleDebug        # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # runs all JVM-level unit tests (no device needed)
./gradlew assembleRelease      # release variant, lint included
```

## Testing

All tests are plain JVM unit tests (`app/src/test`) - no emulator, device,
or Robolectric required, run via `./gradlew testDebugUnitTest`:

| Test class | Covers |
|---|---|
| `api.AuroraHttpServerTest` | `/health`, `/dashboard` (fully populated and all-null/empty cases), 404 - starts a real `AuroraHttpServer` on an ephemeral port and hits it over real HTTP |
| `calendar.CalendarEventMapperTest` | cancelled-event filtering, chronological sorting, HH:mm formatting, all-day UTC handling, null-title safety |
| `alarm.AlarmMapperTest` | null passthrough, trigger-time formatting |
| `weather.WeatherCacheTest` | empty/fresh/stale transitions and the failed-refresh-keeps-old-data path, driven by a fake clock |
| `weather.OpenMeteoResponseParserTest` | parses a real response shape, ignores unknown fields, throws (rather than returning bad data) on malformed JSON |
| `weather.WeatherConditionMapperTest` | WMO weather code → label mapping, including an unrecognized code |

21 tests, all passing as of this writing.

`CalendarRepositoryImpl`, `AlarmRepositoryImpl`, and `WeatherRepositoryImpl`
themselves (the actual `ContentResolver`/`AlarmManager`/network calls) are
intentionally *not* unit tested, same as `BatteryRepositoryImpl` before them
- they're thin Android-integration edges with no real logic in them, and
were instead verified by installing the app on a real device (see below).

### Manual/device verification performed

Installed on a physical Galaxy A13 (Android 14) via `adb`, with the HTTP
server hit from a separate machine on the same LAN:

- `/health` and `/dashboard` both reachable over real Wi-Fi.
- Battery/charging matched `adb shell dumpsys battery`.
- Notification count matched `adb shell dumpsys notification` exactly,
  before and after granting notification access.
- `nextAlarm` matched a real alarm already scheduled on the device.
- Weather returned a real Open-Meteo response for the configured location.
- A temporary event was inserted directly into the Calendar Provider via
  `adb shell content insert`; it appeared correctly in `/dashboard`'s
  `"calendar"` array and in the "Today's Events" card, then was removed
  again and `/dashboard` correctly reverted to `"calendar": []`.
- Calendar permission denial was confirmed not to crash the app - the
  dashboard and UI degrade to `[]` / a "grant access" card as designed.

## Roadmap (v0.3+)

Still not implemented, all following the same repository-interface +
`DashboardResponse`-field pattern established so far: phone volume mode,
Wi-Fi status, charging time estimate, per-app unread counts grouped by
application, morning greeting.

The HTTP server is also expected to move into a foreground `Service` once
"survives the app being backgrounded/killed" matters in practice -
`AuroraServerController`'s API was designed for that move already (see
Architecture above).

Weather's location is currently a hardcoded constant
(`weather/WeatherConfig.kt`) - worth revisiting if Aurora's home location
ever changes, or if a settings screen is added.
