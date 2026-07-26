# Aurora

Aurora is the "brains" behind a custom bedside dashboard: it runs on an
Android phone, reads live data off that phone (battery, notifications, and
eventually weather/calendar/etc.), and serves it as JSON over the local
network to a rooted Echo Show 5 running a kiosk-mode HTML/CSS/JS dashboard.

Phone and Echo Show are assumed to always be on the same Wi-Fi network;
Aurora talks HTTP, not cloud APIs.

## Architecture

```
com.rusty.aurora
├── api/            HTTP layer: NanoHTTPD server + one Route per endpoint
├── battery/        BatteryRepository - wraps android.os.BatteryManager
├── notifications/  NotificationCountRepository + the NotificationListenerService
├── di/             Hand-rolled composition root (AppContainer)
├── model/          Serializable response DTOs, shared enums
├── service/        AuroraServerController - server lifecycle, Activity-agnostic
├── ui/             Compose screen, ViewModel, MainActivity, theme
└── util/           NetworkUtil (LAN IP), NotificationAccessUtil (Settings deep link)
```

### Why these boundaries

- **`api/` never contains business logic.** `AuroraHttpServer` only matches
  an incoming request against a `List<Route>` and delegates. Each `Route`
  (`HealthRoute`, `DashboardRoute`, ...) owns exactly one endpoint. Adding
  a `/weather` endpoint later means writing `WeatherRoute` and listing it
  in `AppContainer` - `AuroraHttpServer` doesn't change.

- **Repositories, not raw Android APIs, are what everything else depends
  on.** `BatteryRepository` and `NotificationCountRepository` are
  interfaces; `DashboardRoute` and `AuroraViewModel` depend on the
  interface, not `BatteryManager`/`NotificationListenerService` directly.
  That's what makes them fake-able in tests and keeps Android framework
  types out of the HTTP and UI layers.

- **`service/AuroraServerController` takes no Activity/Service context.**
  It's constructed with just a `(port: Int) -> AuroraHttpServer` factory.
  Today it's started from `AuroraViewModel`; moving the server into a
  foreground `Service` later (so it survives the app being backgrounded)
  is a change to *where* `start()`/`stop()` are called, not to this class.

- **`di/AppContainer` is a plain object graph, not Hilt/Koin.** One app,
  two repositories, one server - a DI framework would add build time and
  APK size for no benefit at this scale. Every class still takes its
  dependencies through its constructor, so the option to introduce a
  framework later (or just swap in fakes for tests) is still open.

- **`AuroraNotificationListenerService` is the one sanctioned exception**
  to constructor injection. Android instantiates `NotificationListenerService`
  subclasses itself, so it can't receive dependencies through a
  constructor - it reads `(application as AuroraApplication).container`
  instead. It only recomputes a *count* from `activeNotifications`, never
  reads notification content.

## API (v0.1)

`GET /health`
```
Aurora OK
```

`GET /dashboard`
```json
{
  "battery": 82,
  "charging": true,
  "notifications": 4
}
```

Default port is `8080`, shown and copyable from the app UI.

## Notification access

Notification access isn't a runtime-permission dialog - it's a toggle in
system Settings (Settings → Notification access). The app detects when
it's missing and shows a "Grant Notification Access" button that deep
links straight there via `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
Until granted, `notifications` in `/dashboard` reads `0`.

## Building

This was scaffolded in an environment without a JDK/Gradle installed, so
it has **not** been compiled yet. Everything needed to build is here
*except* the Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`),
which is a binary file - `gradle-wrapper.properties` and the version
catalog are in place, but you'll need to do one of:

- Open the project root in Android Studio and let it regenerate the
  wrapper on first sync, or
- Run `gradle wrapper --gradle-version 8.9` from this directory if you
  have a system Gradle install.

After that, `./gradlew assembleDebug` (or Android Studio's Run button)
builds and installs the app.

## Roadmap

Fields listed in the original spec but not yet implemented - weather,
calendar events, next alarm, volume mode, Wi-Fi status, charging time
estimate, per-app unread counts, morning greeting - all follow the same
pattern already established by battery and notifications:

1. A repository interface + impl in a new package (`weather/`, `calendar/`, ...).
2. A new field on `DashboardResponse` (or a new endpoint via a new `Route`,
   for data that doesn't belong on `/dashboard`).
3. The repository listed in `AppContainer` and passed into whichever
   `Route` needs it.

No changes to `AuroraHttpServer`, `AuroraServerController`, or the
routing logic are expected to be needed for any of these.

The HTTP server is also expected to move into a foreground `Service`
once "survives the app being backgrounded/killed" matters in practice -
`AuroraServerController`'s API was designed for that move already (see
Architecture above).
