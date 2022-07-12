package it.unibo.mobilesystems.geo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.unibo.mobilesystems.debugUtils.Debugger
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint

class PathViewModel(
    private val pathCalculator: PathCalculator
) : ViewModel() {

    fun asyncCalculatePath(startPoint : GeoPoint, destinationPoint: GeoPoint,
                           uiUpdateOnRes : (Result<Road>) -> Unit
    ) {
        viewModelScope.launch {
            val result = pathCalculator.calculateRoad(startPoint, destinationPoint)
            uiUpdateOnRes(result)
        }
    }

}