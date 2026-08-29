package com.lifegrouptext

import android.app.Application
import com.lifegrouptext.di.AppContainer

/**
 * Application entry point. Holds the manual dependency container so we avoid a
 * heavyweight DI framework and keep the dependency footprint "light".
 */
class LifeGroupTextsApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
