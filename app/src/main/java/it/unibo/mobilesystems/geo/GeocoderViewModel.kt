package it.unibo.mobilesystems.geo

import android.location.Address
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GeocoderViewModel(
    private val geocoder: Geocoder
) : ViewModel() {

    private val updateUiOnRes = mutableListOf<(Result<List<Address>>) -> Unit>()

    fun addUpdateUiOnResult(action : (Result<List<Address>>) -> Unit) {
        updateUiOnRes.add(action)
    }

    fun removeUpdateUiOnResult(action : (Result<List<Address>>) -> Unit) {
        updateUiOnRes.remove(action)
    }

    fun asyncGetPlacesFromLocation(location : String) {
        viewModelScope.launch {
            val res = geocoder.getPlacesFromLocation(location)
            updateUiOnRes.forEach { action ->
                action(res)
            }
        }
    }

}