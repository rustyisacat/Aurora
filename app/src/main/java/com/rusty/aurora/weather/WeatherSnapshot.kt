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
    // Today plus the next few days (see WeatherConfig.FORECAST_DAYS) -
    // empty only if Open-Meteo's response omitted the daily block's time/
    // weather_code entirely, which shouldn't happen in practice.
    val dailyForecast: List<DailyForecastEntry> = emptyList(),
    // US EPA Air Quality Index (0-500+) for this snapshot's coordinate -
    // null if the lookup failed or hasn't completed yet, same "unknown,
    // not absence of weather" meaning as radarStation below. The
    // dashboard derives the Good/Moderate/... category client-side from
    // the standard EPA breakpoints rather than Aurora doing it, since
    // it's a pure function of the number with nothing else to know.
    val airQualityIndex: Int? = null,
    // 4-letter NWS radar station id (e.g. "KJAX") covering this snapshot's
    // coordinate - null if the lookup failed, same "unknown, not absence
    // of weather" meaning as everything else here. The dashboard uses this
    // to build a radar.weather.gov image URL directly; Aurora never
    // fetches or proxies the radar image itself.
    val radarStation: String? = null
)
