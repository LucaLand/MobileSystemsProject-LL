package it.unibo.mobilesystems.geo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint

class PathViewModel(
    private val pathCalculator: PathCalculator
) : ViewModel() {

    fun asyncCalculatePath(startPoint : GeoPoint, destinationPoint: GeoPoint,
                           uiUpdateOnRes : (Road) -> Unit
    ) {
        viewModelScope.launch {
            val result = pathCalculator.calculateRoad(startPoint, destinationPoint)
            result.onSuccess {
                uiUpdateOnRes(it)
            }
        }
    }

}