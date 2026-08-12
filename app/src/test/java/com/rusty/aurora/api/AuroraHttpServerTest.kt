package com.rusty.aurora.api

import android.net.Uri
import com.rusty.aurora.alarm.AlarmRepository
import com.rusty.aurora.alarm.NextAlarm
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.calendar.CalendarEvent
import com.rusty.aurora.calendar.CalendarRepository
import com.rusty.aurora.calendar.WeekDay
import com.rusty.aurora.layout.DEFAULT_TILE_LAYOUT
import com.rusty.aurora.layout.LayoutRepository
import com.rusty.aurora.layout.TileConfig
import com.rusty.aurora.layout.TileSize
import com.rusty.aurora.network.HomeNetworkRepository
import com.rusty.aurora.notifications.DndRepository
import com.rusty.aurora.notifications.KnownApp
import com.rusty.aurora.notifications.NotificationBlocklistRepository
import com.rusty.aurora.notifications.NotificationCountRepositoryImpl
import com.rusty.aurora.notifications.NotificationGroup
import com.rusty.aurora.photo.WallpaperConfigRepository
import com.rusty.aurora.photo.WallpaperMode
import com.rusty.aurora.photo.WallpaperScheduleEntry
import com.rusty.aurora.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.rusty.aurora.sound.SoundInfo
import com.rusty.aurora.sound.SoundMachineState
import com.rusty.aurora.sound.SoundRepository
import com.rusty.aurora.sound.SoundSource
import com.rusty.aurora.sound.SoundStream
import com.rusty.aurora.wakealarm.WakeAlarm
import com.rusty.aurora.wakealarm.WakeAlarmRepository
import com.rusty.aurora.wakealarm.WakeAlarmRingingState
import com.rusty.aurora.weather.WeatherAlert
import com.rusty.aurora.weather.WeatherAlertRepository
import com.rusty.aurora.weather.WeatherRepository
import com.rusty.aurora.weather.WeatherSnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

/**
 * Exercises AuroraHttpServer end-to-end over real HTTP. None of api/ or the
 * repository interfaces touch the Android framework, so this runs as a
 * plain JVM unit test - no emulator or device required to prove the server
 * actually starts and answers requests correctly.
 */
class AuroraHttpServerTest {

    private val DEFAULT_LAYOUT_JSON =
        """"layout":[{"id":"weather","visible":true,"size":"medium"},""" +
            """{"id":"phone","visible":true,"size":"medium"},""" +
            """{"id":"notifications","visible":true,"size":"large"},""" +
            """{"id":"schedule","visible":true,"size":"medium"},""" +
            """{"id":"alarm","visible":true,"size":"small"},""" +
            """{"id":"sound","visible":true,"size":"large"}],"userName":null,"dndEnabled":false,""" +
            """"chargingEtaMinutes":null,"wallpaperMode":"rotating","wallpaperSinglePhotoId":null,""" +
            """"wallpaperSchedule":[],"weatherAlert":null}"""

    private class FakeBatteryRepository(
        private val level: Int,
        private val charging: Boolean
    ) : BatteryRepository {
        override fun getBatteryLevelPercent(): Int = level
        override fun isCharging(): Boolean = charging
        override fun getChargingEtaMinutes(): Int? = null
    }

    private class FakeCalendarRepository(private val events: List<CalendarEvent>) : CalendarRepository {
        override fun hasCalendarPermission(): Boolean = true
        override fun getEvents(): List<CalendarEvent> = events
        override fun isShowingTomorrow(): Boolean = false
        override fun getWeekEvents(): List<WeekDay> = emptyList()
    }

    private class FakeAlarmRepository(private val alarm: NextAlarm?) : AlarmRepository {
        override fun getNextAlarm(): NextAlarm? = alarm
        override fun getNextAlarmTriggerMillis(): Long? = null
    }

    private class FakeWeatherRepository(private val weather: WeatherSnapshot?) : WeatherRepository {
        override fun getWeather(): WeatherSnapshot? = weather
    }

    private class FakeWeatherAlertRepository(private val alert: WeatherAlert? = null) : WeatherAlertRepository {
        override fun getAlert(): WeatherAlert? = alert
    }

    private class FakeLayoutRepository(private var tiles: List<TileConfig> = DEFAULT_TILE_LAYOUT) : LayoutRepository {
        override fun getLayout(): List<TileConfig> = tiles
        override fun setLayout(tiles: List<TileConfig>) {
            if (tiles.none { it.visible }) return
            this.tiles = tiles
        }
    }

    private class FakeUserProfileRepository(private var name: String? = null) : UserProfileRepository {
        override fun getUserName(): String? = name
        override fun setUserName(name: String) {
            this.name = name
        }
    }

    private class FakeDndRepository(private var enabled: Boolean = false) : DndRepository {
        override fun isEnabled(): Boolean = enabled
        override fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
        }
    }

    private class FakeWallpaperConfigRepository(
        private var mode: WallpaperMode = WallpaperMode.ROTATING,
        private var singlePhotoId: String? = null,
        private var schedule: List<WallpaperScheduleEntry> = emptyList()
    ) : WallpaperConfigRepository {
        override fun getMode(): WallpaperMode = mode
        override fun setMode(mode: WallpaperMode) {
            this.mode = mode
        }
        override fun getSinglePhotoId(): String? = singlePhotoId
        override fun setSinglePhotoId(photoId: String?) {
            singlePhotoId = photoId
        }
        override fun getSchedule(): List<WallpaperScheduleEntry> = schedule
        override fun setSchedule(entries: List<WallpaperScheduleEntry>) {
            schedule = entries
        }
    }

    private class FakeHomeNetworkRepository(private var prefix: String? = null) : HomeNetworkRepository {
        override fun getHomeSubnetPrefix(): String? = prefix
        override fun setHomeSubnetPrefix(prefix: String) {
            this.prefix = prefix
        }
    }

    private class FakeNotificationBlocklistRepository(
        knownApps: List<KnownApp> = emptyList(),
        blocked: Set<String> = emptySet()
    ) : NotificationBlocklistRepository {
        private val _knownApps = MutableStateFlow(knownApps)
        override val knownApps: StateFlow<List<KnownApp>> = _knownApps

        private val _blockedPackages = MutableStateFlow(blocked)
        override val blockedPackages: StateFlow<Set<String>> = _blockedPackages

        override fun recordSeen(packageName: String, label: String) {
            if (_knownApps.value.none { it.packageName == packageName }) {
                _knownApps.value = _knownApps.value + KnownApp(packageName, label)
            }
        }

        override fun setBlocked(packageName: String, blocked: Boolean) {
            _blockedPackages.value = if (blocked) _blockedPackages.value + packageName else _blockedPackages.value - packageName
        }
    }

    private class FakeWakeAlarmRepository(
        private var alarms: List<WakeAlarm> = emptyList(),
        private var ringing: WakeAlarmRingingState = WakeAlarmRingingState()
    ) : WakeAlarmRepository {
        var lastDeletedId: String? = null
            private set
        var dismissCalled = false
            private set
        var lastSnoozeMinutes: Int? = null
            private set

        override fun getAlarms(): List<WakeAlarm> = alarms
        override fun setAlarm(alarm: WakeAlarm) {
            alarms = alarms.filterNot { it.id == alarm.id } + alarm
        }
        override fun deleteAlarm(id: String) {
            lastDeletedId = id
            alarms = alarms.filterNot { it.id == id }
        }
        override fun getRingingState(): WakeAlarmRingingState = ringing
        override fun handleFired(alarmId: String) = throw UnsupportedOperationException("not exercised by these tests")
        override fun dismiss() {
            dismissCalled = true
            ringing = WakeAlarmRingingState()
        }
        override fun snooze(minutes: Int) {
            lastSnoozeMinutes = minutes
            ringing = WakeAlarmRingingState()
        }
        override fun rearmAll() = throw UnsupportedOperationException("not exercised by these tests")
        override fun getEarliestEnabledTriggerMillis(): Long? = null

        private var defaultAlarmSoundId: String? = null
        override fun getDefaultAlarmSoundId(): String? = defaultAlarmSoundId
        override fun setDefaultAlarmSoundId(id: String) {
            defaultAlarmSoundId = id
        }
    }

    private class FakeSoundRepository(
        private var state: SoundMachineState = SoundMachineState(
            playing = false,
            sound = null,
            volume = 50,
            sleepTimerMinutes = null
        ),
        private val library: List<SoundInfo> = listOf(
            SoundInfo("rain", "Rain", SoundSource.Asset("sounds/rain.mp3"))
        )
    ) : SoundRepository {
        var lastPlayedId: String? = null
            private set
        var lastVolumeSet: Int? = null
            private set
        var lastTimerPreset: String? = null
            private set
        var pauseCalled = false
            private set
        var stopCalled = false
            private set

        override fun getState(): SoundMachineState = state
        override fun getLibrary(): List<SoundInfo> = library

        override fun play(soundId: String?) {
            lastPlayedId = soundId
            state = state.copy(playing = true)
        }

        override fun pause() {
            pauseCalled = true
            state = state.copy(playing = false)
        }

        override fun stop() {
            stopCalled = true
            state = state.copy(playing = false)
        }

        override fun setVolume(percent: Int) {
            lastVolumeSet = percent
            state = state.copy(volume = percent)
        }

        override fun setSleepTimer(rawPreset: String) {
            lastTimerPreset = rawPreset
        }

        override fun importCustomSound(uri: Uri, displayName: String): SoundInfo =
            throw UnsupportedOperationException("not exercised by these tests")

        override fun openSoundStream(soundId: String): SoundStream? =
            if (soundId == "rain") SoundStream(byteArrayOf(1, 2, 3, 4), "audio/mpeg") else null
    }

    private var server: AuroraHttpServer? = null
    private lateinit var fakeSoundRepository: FakeSoundRepository
    private lateinit var fakeWakeAlarmRepository: FakeWakeAlarmRepository
    private lateinit var fakeLayoutRepository: FakeLayoutRepository
    private lateinit var fakeUserProfileRepository: FakeUserProfileRepository
    private lateinit var fakeHomeNetworkRepository: FakeHomeNetworkRepository
    private lateinit var fakeNotificationBlocklistRepository: FakeNotificationBlocklistRepository
    private lateinit var fakeWallpaperConfigRepository: FakeWallpaperConfigRepository
    private lateinit var fakeNotificationCountRepository: NotificationCountRepositoryImpl

    @After
    fun stopServer() {
        server?.stop()
    }

    private fun startServer(
        calendarEvents: List<CalendarEvent> = emptyList(),
        nextAlarm: NextAlarm? = null,
        weather: WeatherSnapshot? = null,
        notificationGroups: List<NotificationGroup> = emptyList(),
        wakeAlarms: List<WakeAlarm> = emptyList(),
        wakeAlarmRinging: WakeAlarmRingingState = WakeAlarmRingingState(),
        knownApps: List<KnownApp> = emptyList(),
        blockedPackages: Set<String> = emptySet()
    ): String {
        fakeSoundRepository = FakeSoundRepository()
        fakeWakeAlarmRepository = FakeWakeAlarmRepository(wakeAlarms, wakeAlarmRinging)
        fakeLayoutRepository = FakeLayoutRepository()
        fakeUserProfileRepository = FakeUserProfileRepository()
        fakeHomeNetworkRepository = FakeHomeNetworkRepository()
        fakeNotificationBlocklistRepository = FakeNotificationBlocklistRepository(knownApps, blockedPackages)
        fakeWallpaperConfigRepository = FakeWallpaperConfigRepository()
        fakeNotificationCountRepository = NotificationCountRepositoryImpl().apply { update(notificationGroups) }
        val routes = listOf(
            HealthRoute(),
            DashboardRoute(
                batteryRepository = FakeBatteryRepository(level = 77, charging = true),
                notificationCountRepository = fakeNotificationCountRepository,
                calendarRepository = FakeCalendarRepository(calendarEvents),
                alarmRepository = FakeAlarmRepository(nextAlarm),
                weatherRepository = FakeWeatherRepository(weather),
                soundRepository = fakeSoundRepository,
                wakeAlarmRepository = fakeWakeAlarmRepository,
                layoutRepository = fakeLayoutRepository,
                userProfileRepository = fakeUserProfileRepository,
                dndRepository = FakeDndRepository(),
                wallpaperConfigRepository = fakeWallpaperConfigRepository,
                weatherAlertRepository = FakeWeatherAlertRepository()
            ),
            PlaySoundRoute(fakeSoundRepository),
            PauseSoundRoute(fakeSoundRepository),
            StopSoundRoute(fakeSoundRepository),
            SetVolumeRoute(fakeSoundRepository),
            SetSleepTimerRoute(fakeSoundRepository),
            SoundLibraryRoute(fakeSoundRepository),
            SoundStreamRoute(fakeSoundRepository),
            GetWakeAlarmsRoute(fakeWakeAlarmRepository),
            SetWakeAlarmRoute(fakeWakeAlarmRepository),
            DeleteWakeAlarmRoute(fakeWakeAlarmRepository),
            DismissWakeAlarmRoute(fakeWakeAlarmRepository),
            SnoozeWakeAlarmRoute(fakeWakeAlarmRepository),
            SetUserNameRoute(fakeUserProfileRepository),
            GetHomeNetworkRoute(fakeHomeNetworkRepository),
            SetHomeNetworkRoute(fakeHomeNetworkRepository),
            KnownNotificationAppsRoute(fakeNotificationBlocklistRepository),
            SetNotificationBlockedRoute(fakeNotificationBlocklistRepository, fakeNotificationCountRepository),
            SetWallpaperModeRoute(fakeWallpaperConfigRepository),
            SetWallpaperSinglePhotoRoute(fakeWallpaperConfigRepository),
            SetWallpaperScheduleRoute(fakeWallpaperConfigRepository),
            SetLayoutRoute(fakeLayoutRepository)
        )

        // Port 0: the OS assigns a free ephemeral port, so tests never collide
        // with each other or with a real Aurora instance running locally.
        val newServer = AuroraHttpServer(port = 0, routes = routes)
        newServer.start(AuroraHttpServer.DEFAULT_SOCKET_TIMEOUT_MS, true)
        server = newServer
        return "http://localhost:${newServer.listeningPort}"
    }

    @Test
    fun `health endpoint returns 200 and plain text OK body`() {
        val baseUrl = startServer()
        val connection = get("$baseUrl/health")

        assertEquals(200, connection.responseCode)
        assertEquals("Aurora OK", connection.inputStream.bufferedReader().readText())
    }

    @Test
    fun `dashboard endpoint includes battery, grouped notifications, alarm, calendar, weather, and sound machine`() {
        val baseUrl = startServer(
            calendarEvents = listOf(CalendarEvent(title = "School", start = "08:00", end = "15:00", allDay = false)),
            nextAlarm = NextAlarm(time = "07:00", enabled = true),
            weather = WeatherSnapshot(
                temperature = 74, condition = "Clear", high = 86, low = 68,
                timezone = "America/New_York", sunrise = "06:15", sunset = "20:42"
            ),
            notificationGroups = listOf(
                NotificationGroup(
                    app = "Discord",
                    packageName = "com.discord",
                    count = 3,
                    latestTitle = "Alice",
                    latestText = "hey"
                )
            )
        )
        val connection = get("$baseUrl/dashboard")
        val body = connection.inputStream.bufferedReader().readText()

        assertEquals(200, connection.responseCode)
        assertTrue(connection.contentType.startsWith("application/json"))
        assertEquals(
            """{"battery":77,"charging":true,"notifications":3,""" +
                """"notificationGroups":[{"app":"Discord","packageName":"com.discord","count":3,""" +
                """"latestTitle":"Alice","latestText":"hey"}],""" +
                """"nextAlarm":{"time":"07:00","enabled":true},""" +
                """"calendar":[{"title":"School","start":"08:00","end":"15:00","allDay":false}],""" +
                """"calendarShowsTomorrow":false,"weekCalendar":[],""" +
                """"weather":{"temperature":74,"condition":"Clear","high":86,"low":68,"timezone":"America/New_York","sunrise":"06:15","sunset":"20:42","rainExpectedAt":null,"precipitationProbability":null,"feelsLike":null,"windSpeedMph":null,"humidityPercent":null,"uvIndex":null,"dailyForecast":[],"airQualityIndex":null,"radarStation":null},""" +
                """"soundMachine":{"playing":false,"sound":null,"volume":50,"sleepTimerMinutes":null},""" +
                """"wakeAlarms":[],""" +
                """"wakeAlarmRinging":{"ringing":false,"alarmId":null,"label":"","soundId":null},""" +
                """"defaultAlarmSoundId":null,""" +
                DEFAULT_LAYOUT_JSON,
            body
        )
    }

    @Test
    fun `dashboard endpoint represents absent alarm and weather as explicit json null, not missing keys`() {
        val baseUrl = startServer(calendarEvents = emptyList(), nextAlarm = null, weather = null)
        val connection = get("$baseUrl/dashboard")
        val body = connection.inputStream.bufferedReader().readText()

        assertEquals(
            """{"battery":77,"charging":true,"notifications":0,"notificationGroups":[],""" +
                """"nextAlarm":null,"calendar":[],"calendarShowsTomorrow":false,"weekCalendar":[],"weather":null,""" +
                """"soundMachine":{"playing":false,"sound":null,"volume":50,"sleepTimerMinutes":null},""" +
                """"wakeAlarms":[],""" +
                """"wakeAlarmRinging":{"ringing":false,"alarmId":null,"label":"","soundId":null},""" +
                """"defaultAlarmSoundId":null,""" +
                DEFAULT_LAYOUT_JSON,
            body
        )
    }

    @Test
    fun `unknown route returns 404`() {
        val baseUrl = startServer()
        val connection = get("$baseUrl/nonexistent")

        assertEquals(404, connection.responseCode)
    }

    @Test
    fun `responses include CORS headers so a browser fetch from the dashboard's origin can read them`() {
        val baseUrl = startServer()
        val connection = get("$baseUrl/dashboard")
        connection.responseCode // force the request to actually execute

        assertEquals("*", connection.getHeaderField("Access-Control-Allow-Origin"))
        assertEquals("true", connection.getHeaderField("Access-Control-Allow-Private-Network"))
        assertTrue(connection.getHeaderField("Access-Control-Allow-Methods").contains("POST"))
    }

    @Test
    fun `OPTIONS preflight is answered directly with CORS headers, without going through routing`() {
        val baseUrl = startServer()
        val connection = (URL("$baseUrl/dashboard").openConnection() as HttpURLConnection).apply {
            requestMethod = "OPTIONS"
            connectTimeout = 2000
            readTimeout = 2000
        }

        assertEquals(200, connection.responseCode)
        assertEquals("*", connection.getHeaderField("Access-Control-Allow-Origin"))
    }

    @Test
    fun `play route passes the id query param through to the repository`() {
        val baseUrl = startServer()
        val connection = post("$baseUrl/sound/play?id=rain")

        assertEquals(200, connection.responseCode)
        assertEquals("rain", fakeSoundRepository.lastPlayedId)
    }

    @Test
    fun `play route with no id passes null, letting the repository resume the last sound`() {
        val baseUrl = startServer()
        post("$baseUrl/sound/play").responseCode

        assertNull(fakeSoundRepository.lastPlayedId)
    }

    @Test
    fun `pause and stop routes call through to the repository`() {
        val baseUrl = startServer()

        assertEquals(200, post("$baseUrl/sound/pause").responseCode)
        assertTrue(fakeSoundRepository.pauseCalled)

        assertEquals(200, post("$baseUrl/sound/stop").responseCode)
        assertTrue(fakeSoundRepository.stopCalled)
    }

    @Test
    fun `volume route parses the value param and calls setVolume`() {
        val baseUrl = startServer()
        val connection = post("$baseUrl/sound/volume?value=42")

        assertEquals(200, connection.responseCode)
        assertEquals(42, fakeSoundRepository.lastVolumeSet)
    }

    @Test
    fun `volume route rejects a missing or non-numeric value with 400`() {
        val baseUrl = startServer()

        assertEquals(400, post("$baseUrl/sound/volume").responseCode)
        assertEquals(400, post("$baseUrl/sound/volume?value=loud").responseCode)
    }

    @Test
    fun `timer route passes the preset param through, defaulting to off`() {
        val baseUrl = startServer()

        post("$baseUrl/sound/timer?preset=60").responseCode
        assertEquals("60", fakeSoundRepository.lastTimerPreset)

        post("$baseUrl/sound/timer").responseCode
        assertEquals("off", fakeSoundRepository.lastTimerPreset)
    }

    @Test
    fun `library route returns the catalog as id-displayName pairs`() {
        val baseUrl = startServer()
        val connection = get("$baseUrl/sound/library")

        assertEquals(200, connection.responseCode)
        assertEquals(
            """[{"id":"rain","displayName":"Rain"}]""",
            connection.inputStream.bufferedReader().readText()
        )
    }

    @Test
    fun `stream route serves the raw bytes with the correct content type`() {
        val baseUrl = startServer()
        val connection = get("$baseUrl/sound/stream?id=rain")

        assertEquals(200, connection.responseCode)
        assertEquals("audio/mpeg", connection.contentType)
        assertEquals(4, connection.inputStream.readBytes().size)
    }

    @Test
    fun `stream route 404s for an unknown sound id`() {
        val baseUrl = startServer()
        assertEquals(404, get("$baseUrl/sound/stream?id=nonexistent").responseCode)
    }

    @Test
    fun `stream route 400s when id is missing`() {
        val baseUrl = startServer()
        assertEquals(400, get("$baseUrl/sound/stream").responseCode)
    }

    @Test
    fun `name route sets the user name, reflected on the next dashboard poll`() {
        val baseUrl = startServer()

        assertEquals(200, post("$baseUrl/settings/name?value=Rusty").responseCode)
        assertEquals("Rusty", fakeUserProfileRepository.getUserName())
        assertTrue(get("$baseUrl/dashboard").inputStream.bufferedReader().readText().contains(""""userName":"Rusty""""))
    }

    @Test
    fun `name route rejects a missing or blank value with 400`() {
        val baseUrl = startServer()

        assertEquals(400, post("$baseUrl/settings/name").responseCode)
        assertEquals(400, post("$baseUrl/settings/name?value=").responseCode)
    }

    @Test
    fun `home-network route round-trips a valid subnet prefix`() {
        val baseUrl = startServer()

        assertEquals(200, post("$baseUrl/settings/home-network?prefix=192.168.1").responseCode)
        assertEquals(
            """{"prefix":"192.168.1."}""",
            get("$baseUrl/settings/home-network").inputStream.bufferedReader().readText()
        )
    }

    @Test
    fun `home-network route rejects a malformed prefix with 400`() {
        val baseUrl = startServer()

        assertEquals(400, post("$baseUrl/settings/home-network?prefix=not-an-ip").responseCode)
        assertEquals(400, post("$baseUrl/settings/home-network?prefix=999.1.1").responseCode)
        assertNull(fakeHomeNetworkRepository.getHomeSubnetPrefix())
    }

    @Test
    fun `known notification apps route reports each app's current blocked state`() {
        val baseUrl = startServer(
            knownApps = listOf(KnownApp("com.discord", "Discord"), KnownApp("com.life360", "Life360")),
            blockedPackages = setOf("com.life360")
        )
        val connection = get("$baseUrl/notifications/apps")

        assertEquals(200, connection.responseCode)
        assertEquals(
            """[{"packageName":"com.discord","label":"Discord","blocked":false},""" +
                """{"packageName":"com.life360","label":"Life360","blocked":true}]""",
            connection.inputStream.bufferedReader().readText()
        )
    }

    @Test
    fun `block route toggles an app's blocked state`() {
        val baseUrl = startServer(knownApps = listOf(KnownApp("com.discord", "Discord")))

        assertEquals(200, post("$baseUrl/notifications/block?package=com.discord&blocked=true").responseCode)
        assertEquals(setOf("com.discord"), fakeNotificationBlocklistRepository.blockedPackages.value)

        assertEquals(200, post("$baseUrl/notifications/block?package=com.discord&blocked=false").responseCode)
        assertTrue(fakeNotificationBlocklistRepository.blockedPackages.value.isEmpty())
    }

    @Test
    fun `block route rejects missing package or invalid blocked value with 400`() {
        val baseUrl = startServer()

        assertEquals(400, post("$baseUrl/notifications/block?blocked=true").responseCode)
        assertEquals(400, post("$baseUrl/notifications/block?package=com.discord").responseCode)
        assertEquals(400, post("$baseUrl/notifications/block?package=com.discord&blocked=maybe").responseCode)
    }

    @Test
    fun `wallpaper mode route accepts the three valid modes and rejects anything else`() {
        val baseUrl = startServer()

        assertEquals(200, post("$baseUrl/photos/wallpaper/mode?mode=single").responseCode)
        assertEquals(WallpaperMode.SINGLE, fakeWallpaperConfigRepository.getMode())

        assertEquals(200, post("$baseUrl/photos/wallpaper/mode?mode=scheduled").responseCode)
        assertEquals(WallpaperMode.SCHEDULED, fakeWallpaperConfigRepository.getMode())

        assertEquals(400, post("$baseUrl/photos/wallpaper/mode?mode=nonsense").responseCode)
    }

    @Test
    fun `wallpaper single-photo route sets and clears the selection`() {
        val baseUrl = startServer()

        assertEquals(200, post("$baseUrl/photos/wallpaper/single?photoId=abc123").responseCode)
        assertEquals("abc123", fakeWallpaperConfigRepository.getSinglePhotoId())

        assertEquals(200, post("$baseUrl/photos/wallpaper/single").responseCode)
        assertNull(fakeWallpaperConfigRepository.getSinglePhotoId())
    }

    @Test
    fun `wallpaper schedule route accepts a JSON body list of entries`() {
        val baseUrl = startServer()
        val connection = postJson(
            "$baseUrl/photos/wallpaper/schedule",
            """[{"photoId":"abc","time":"07:00"},{"photoId":"def","time":"20:00"}]"""
        )

        assertEquals(200, connection.responseCode)
        assertEquals(
            listOf(WallpaperScheduleEntry("abc", "07:00"), WallpaperScheduleEntry("def", "20:00")),
            fakeWallpaperConfigRepository.getSchedule()
        )
    }

    @Test
    fun `wallpaper schedule route rejects malformed JSON with 400`() {
        val baseUrl = startServer()
        assertEquals(400, postJson("$baseUrl/photos/wallpaper/schedule", "not json").responseCode)
    }

    @Test
    fun `layout route replaces the layout with a valid JSON body`() {
        val baseUrl = startServer()
        val connection = postJson(
            "$baseUrl/layout",
            """[{"id":"weather","visible":true,"size":"small"}]"""
        )

        assertEquals(200, connection.responseCode)
        assertEquals(
            listOf(TileConfig("weather", visible = true, size = TileSize.SMALL)),
            fakeLayoutRepository.getLayout()
        )
    }

    @Test
    fun `layout route silently rejects a layout that would leave zero visible tiles`() {
        val baseUrl = startServer()
        // Prove the guard actually rejects the write, not just that nothing
        // ever gets through: apply a real change first, then confirm the
        // all-invisible attempt leaves *that* in place rather than the
        // original default layout.
        postJson("$baseUrl/layout", """[{"id":"weather","visible":true,"size":"small"}]""").responseCode
        val afterValidWrite = fakeLayoutRepository.getLayout()

        val connection = postJson(
            "$baseUrl/layout",
            """[{"id":"weather","visible":false,"size":"medium"}]"""
        )

        assertEquals(200, connection.responseCode)
        assertEquals(afterValidWrite, fakeLayoutRepository.getLayout())
    }

    private fun get(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2000
            readTimeout = 2000
        }

    private fun post(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 2000
            readTimeout = 2000
        }

    private fun postJson(url: String, body: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 2000
            readTimeout = 2000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
}
