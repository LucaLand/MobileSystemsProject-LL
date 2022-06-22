package it.unibo.mobilesystems.actors

import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import com.google.gson.Gson
import it.unibo.kactor.QActorBasicFsm
import it.unibo.kactor.annotations.Initial
import it.unibo.kactor.annotations.QActor
import it.unibo.kactor.annotations.State
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.utils.SystemLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val LOCATION_EVENT_NAME = "new_location"

@QActor(GIT_BERTO_CTX_NAME)
class LocationManagerActor() : QActorBasicFsm() {

    private val gson = Gson()

    private lateinit var locationListener: LocationListener

    @State
    @Initial
    suspend fun start() {
        this.actor.tt
        locationListener = LocationListener { location ->
            this@LocationManagerActor.actor.scope.launch {
                Debugger.printDebug(name, actorStringln("Location: $location"))
                emit event LOCATION_EVENT_NAME withContent gson.toJson(location)
            }
        }
        Debugger.printDebug(name, actorStringln("waiting for location manager..."))
        withContext(Dispatchers.Main) {
            SystemLocationManager.getSystemLocationManager().requestLocationUpdates(getLocationProvider(), 500,
                0.5F, locationListener)
        }
        Debugger.printDebug(name, actorStringln("Started"))
    }

    private fun getLocationProvider() : String {
        //Set Provider
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationManager.FUSED_PROVIDER
        }else{
            LocationManager.NETWORK_PROVIDER //locationManager.getBestProvider()
        }
    }

}