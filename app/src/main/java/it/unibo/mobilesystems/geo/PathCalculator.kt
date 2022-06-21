package it.unibo.mobilesystems.geo

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.*
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint

class PathCalculator(
    private val roadManager : RoadManager,
    private val scope : CoroutineScope = GlobalScope,
    private val mainDispatcher : CoroutineDispatcher = Dispatchers.Main
) {

    private val onPathCalculated = mutableListOf<(Road) -> Unit>()
    private val onPathCalculatedUiUpdates = mutableListOf<(Road) -> Unit>()

    fun addOnPathCalculated(action : (Road) -> Unit) {
        onPathCalculated.add(action)
    }

    fun addOnPathCalculatedUiUpdate(action : (Road) -> Unit) {
        onPathCalculatedUiUpdates.add(action)
    }

    fun requestPathCalculation(startPoint : GeoPoint, destPoint: GeoPoint){
        scope.launch {
            val road = obtainRoad(startPoint, destPoint)
            onPathCalculated.forEach { it(road) }
            onPathCalculatedUiUpdates.forEach { update ->
                withContext(mainDispatcher) {
                    update(road)
                }
            }
        }
    }

    private fun obtainRoad(startPoint: GeoPoint, endPoint: GeoPoint): Road{
        var waypoints = ArrayList<GeoPoint>()
        waypoints.add(startPoint)
        waypoints.add(endPoint)

        return roadManager.getRoad(waypoints)
    }

}