package it.unibo.mobilesystems.utils

import android.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow

object SystemLocationManager {
    private var systemLocationManager : LocationManager? = null

    fun setSystemLocationManager(locationManager: LocationManager) {
        if(this.systemLocationManager != null)
            throw IllegalStateException("System location manager already set")

        this.systemLocationManager = locationManager
    }

    fun getSystemLocationManager() : LocationManager {
        if(this.systemLocationManager == null)
            throw IllegalStateException("System location manager not set")

        return this.systemLocationManager!!
    }

}