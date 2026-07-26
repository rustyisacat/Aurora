package com.rusty.aurora.di

import android.content.Context
import com.rusty.aurora.api.AuroraHttpServer
import com.rusty.aurora.api.DashboardRoute
import com.rusty.aurora.api.HealthRoute
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.battery.BatteryRepositoryImpl
import com.rusty.aurora.notifications.NotificationCountRepository
import com.rusty.aurora.notifications.NotificationCountRepositoryImpl
import com.rusty.aurora.service.AuroraServerController

/**
 * Hand-rolled composition root.
 *
 * A single app with two backing repositories doesn't need Hilt/Koin - a
 * plain container built once in [com.rusty.aurora.AuroraApplication]
 * gives the same benefit (everything takes its dependencies through its
 * constructor, so tests can pass fakes) without the extra build-time and
 * APK-size cost of a DI framework. Revisit this if the dependency graph
 * grows past what's comfortable to wire by hand.
 */
class AppContainer(context: Context) {

    val batteryRepository: BatteryRepository = BatteryRepositoryImpl(context)

    val notificationCountRepository: NotificationCountRepository = NotificationCountRepositoryImpl()

    private val routes = listOf(
        HealthRoute(),
        DashboardRoute(batteryRepository, notificationCountRepository)
    )

    val serverController = AuroraServerController { port -> AuroraHttpServer(port, routes) }
}
