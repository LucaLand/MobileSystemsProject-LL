package it.unibo.mobilesystems.actors

import android.location.LocationListener
import com.google.android.gms.location.*
import com.google.gson.Gson
import it.unibo.kactor.QActorBasicFsm
import it.unibo.kactor.annotations.*
import it.unibo.kactor.IQActorBasic.*
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.geo.KLocation
import it.unibo.mobilesystems.utils.ApplicationVals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val LOCATION_MANAGER_ACTOR_NAME = "locationmanageractor"

const val LOCATION_EVENT_NAME = "newLocation"
const val ENABLE_LOCATION_MONITORING_ARG = "enableMonitoring"
const val DISABLE_LOCATION_MONITORING_ARG = "disableMonitoring"
const val LMA_CMD_MESSAGE_NAME = "cmd"

@QActor(GIT_BERTO_CTX_NAME, LOCATION_MANAGER_ACTOR_NAME)
class LocationManagerActor() : QActorBasicFsm() {

    companion object {
        val locationRequest = LocationRequest.create().apply {
            interval = 500
            fastestInterval = 1000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
    }

    private val gson = Gson()
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            Debugger.printDebug(name, actorStringln("LocationResult : $p0"))
            p0 ?: return
            actor.scope.launch {
                for (location in p0.locations)
                    emit event LOCATION_EVENT_NAME withContent gson.toJson(KLocation(location))
            }
        }
    }

    private lateinit var locationListener: LocationListener

    @State
    @Initial
    @WhenDispatch("begin2getLocationClient", "getLocationClient", "updateLocationClient")
    suspend fun begin() {
        Debugger.printDebug(name, actorStringln("started. Waiting for location client..."))
    }

    @State
    @EpsilonMove("getLocationClient2idle", "idle")
    suspend fun getLocationClient() {
        fusedLocationClient = ApplicationVals.fusedLocationServices.get()
        Debugger.printDebug(name, actorStringln("location client: $fusedLocationClient"))
    }

    @State
    @WhenDispatch("idle2monitorLocation", "handleCmd", LMA_CMD_MESSAGE_NAME)
    suspend fun idle() {
        Debugger.printDebug(name, actorStringln("idle, waiting for command"))
    }

    @State
    @EpsilonMove("handleCmd2idle", "idle")
    suspend fun handleCmd() {
        Debugger.printDebug(name, actorStringln("handleCmd - currentMsg: $currentMsg"))
        when(currentMsg.msgContent()) {
            ENABLE_LOCATION_MONITORING_ARG -> {
                Debugger.printDebug(name, actorStringln("enabling location monitoring..."))
                withContext(Dispatchers.Main) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        actor.scope.launch {
                            emit event LOCATION_EVENT_NAME withContent gson.toJson(KLocation(location))
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest, locationCallback, null
                    )
                }
                Debugger.printDebug(name, actorStringln("location monitoring is enabled"))
            }

            DISABLE_LOCATION_MONITORING_ARG -> {
                Debugger.printDebug(name, actorStringln("disabling location monitoring"))
                withContext(Dispatchers.Main) {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
                Debugger.printDebug(name, actorStringln("location monitoring is disabled"))
            }
        }
    }

    /*
    @State
    @Initial
    suspend fun start() {
        locationListener = LocationListener { location ->
            this@LocationManagerActor.actor.scope.launch {
                Debugger.printDebug(name, actorStringln("Location: $location"))
                emit event LOCATION_EVENT_NAME withContent gson.toJson(KLocation(location))
            }
        }
        Debugger.printDebug(name, actorStringln("waiting for location manager..."))
        val locReq = com.google.android.gms.location.LocationRequest.create()
        withContext(Dispatchers.Main) {
            ApplicationVals.systemLocationManager.get().requestLocationUpdates(getLocationProvider(), 500,
                0.1F, locationListener)
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
    }*/

}