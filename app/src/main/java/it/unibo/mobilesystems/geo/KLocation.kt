package it.unibo.mobilesystems.geo

import android.location.Location

data class KLocation(
    val latitude : Double,
    val longitude : Double,
    val altitude : Double,
    val accuracy : Float,
    val bearing : Float,
    val bearingAccuracyDegrees : Float,
    val speed : Float,
    val speedAccuracyMetersPerSecond : Float,
    val time : Long,
    val verticalAccuracyMeters : Float,

) {
    constructor(location: Location) : this(location.latitude, location.longitude, location.altitude,
        location.accuracy, location.bearing, location.bearingAccuracyDegrees, location.speed,
        location.speedAccuracyMetersPerSecond, location.time, location.verticalAccuracyMeters
    )
}