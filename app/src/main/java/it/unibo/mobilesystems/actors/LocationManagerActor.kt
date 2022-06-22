package it.unibo.mobilesystems.actors

import android.location.Location
import android.location.LocationListener
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.gson.Gson
import it.unibo.kactor.QActorBasicFsm
import it.unibo.kactor.annotations.EpsilonMove
import it.unibo.kactor.annotations.Initial
import it.unibo.kactor.annotations.QActor
import it.unibo.kactor.annotations.State
import it.unibo.mobilesystems.debugUtils.Debugger
import kotlinx.coroutines.launch

@QActor("ctxgitberto")
class LocationManagerActor() : QActorBasicFsm() {

    private val gson = Gson()

    private lateinit var locationCallback: LocationListener

    @State
    @Initial
    suspend fun start() {
        this.actor.tt
        locationCallback = LocationListener { location ->
            this@LocationManagerActor.actor.scope.launch {
                Debugger.printDebug(name, actorStringln("Location: $location"))
                emit event "location" withContent gson.toJson(location)
            }
        }
        Debugger.printDebug(name, actorStringln("Started"))
    }

}