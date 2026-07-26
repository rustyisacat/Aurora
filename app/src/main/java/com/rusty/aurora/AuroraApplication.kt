package com.rusty.aurora

import android.app.Application
import com.rusty.aurora.di.AppContainer

/**
 * Composition root. Builds [AppContainer] once, on process start, so every
 * component that needs a repository or the server controller - the
 * activity, the system-instantiated notification listener service - reads
 * it from here rather than constructing its own.
 */
class AuroraApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
