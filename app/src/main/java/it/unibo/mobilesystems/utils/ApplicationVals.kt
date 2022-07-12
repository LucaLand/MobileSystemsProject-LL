package it.unibo.mobilesystems.utils

import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow

object ApplicationVals {
    val systemLocationManager = Singleton<LocationManager>()
    val fusedLocationServices = Singleton<FusedLocationProviderClient>()


}