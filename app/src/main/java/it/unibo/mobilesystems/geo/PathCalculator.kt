package it.unibo.mobilesystems.geo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint

class PathCalculator(private val roadManager : RoadManager) {

    /*private val onPathCalculated = mutableListOf<(Road) -> Unit>()
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
    }*/

    suspend fun calculateRoad(startPoint: GeoPoint, endPoint: GeoPoint): Result<Road>{
        return withContext(Dispatchers.IO) {
            val waypoints = ArrayList<GeoPoint>()
            waypoints.add(startPoint)
            waypoints.add(endPoint)

            return@withContext try {
                Result.success(roadManager.getRoad(waypoints))
            } catch (e : Exception) {
                Result.failure(e)
            }
        }
    }

}