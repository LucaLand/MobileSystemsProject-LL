package it.unibo.mobilesystems.actors

import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.gson.Gson
import it.unibo.kactor.QActorBasic
import it.unibo.kactor.annotations.Initial
import it.unibo.kactor.annotations.QActor
import it.unibo.kactor.annotations.State
import it.unibo.mobilesystems.debugUtils.Debugger
import kotlinx.coroutines.launch

@QActor("ctxgitberto")
class LocationManagerActor() : QActorBasic() {

    private val gson = Gson()

    private lateinit var locationCallback: LocationCallback

    @State
    @Initial
    suspend fun start() {
        this.actor.tt
        locationCallback = object : LocationCallback() {

            override fun onLocationResult(p0: LocationResult) {
                this@LocationManagerActor.actor.scope.launch {
                    Debugger.printDebug(name, actorStringln("Location: ${p0.lastLocation}"))
                    emit event "locationResult" withContent gson.toJson(p0.lastLocation)
                }
            }

            override fun onLocationAvailability(p0: LocationAvailability) {
                this@LocationManagerActor.actor.scope.launch {
                    Debugger.printDebug(name, actorStringln("LocationAvailability: $p0"))
                    emit event "locationAvailability" withContent gson.toJson(p0)
                }
            }

        }
        Debugger.printDebug(name, actorStringln("Started"))
    }

}