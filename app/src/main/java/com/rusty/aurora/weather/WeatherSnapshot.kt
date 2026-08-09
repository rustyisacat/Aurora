package com.rusty.aurora.weather

import kotlinx.serialization.Serializable

@Serializable
data class WeatherSnapshot(
    val temperature: Int,
    val condition: String,
    val high: Int,
    val low: Int,
    // IANA name (e.g. "America/New_York") for whatever coordinate this
    // snapshot was fetched for - the dashboard's clock uses this so it
    // always matches wherever the phone actually is, not just whatever
    // system timezone the display device happens to be set to.
    val timezone: String,
    // "HH:mm" 24-hour, same convention as NextAlarm.time/CalendarEvent.start -
    // null only if Open-Meteo's response omitted them, which shouldn't
    // happen in practice since the request always asks for both.
    val sunrise: String? = null,
    val sunset: String? = null,
    // "HH:mm" 24-hour - the first hour today (now or later) whose rain
    // probability clears OpenMeteoResponseParser's threshold. Null means
    // no rain expected today, not "unknown" - the dashboard uses this
    // directly to decide whether to show an umbrella heads-up.
    val rainExpectedAt: String? = null,
    // 4-letter NWS radar station id (e.g. "KJAX") covering this snapshot's
    // coordinate - null if the lookup failed, same "unknown, not absence
    // of weather" meaning as everything else here. The dashboard uses this
    // to build a radar.weather.gov image URL directly; Aurora never
    // fetches or proxies the radar image itself.
    val radarStation: String? = null
)
