package it.unibo.mobilesystems.geo

import android.location.Address
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

class GeocoderViewModel(
    private val geocoder: Geocoder
) : ViewModel() {

    private val updateUiOnRes = mutableListOf<(Result<List<Address>>) -> Unit>()
    private var searchJob : Job? = null
    var lastrResult : Result<List<Address>> = Result.failure(Exception("No calculation has been done"))
    private set

    fun addUpdateUiOnResult(action : (Result<List<Address>>) -> Unit) {
        updateUiOnRes.add(action)
    }

    fun removeUpdateUiOnResult(action : (Result<List<Address>>) -> Unit) {
        updateUiOnRes.remove(action)
    }

    fun asyncGetPlacesFromLocation(location : String) {
        if(searchJob != null) {
            if(searchJob!!.isActive)
                searchJob!!.cancel()
        }

        searchJob = viewModelScope.launch {
            val res = geocoder.getPlacesFromLocation(location)
            this@GeocoderViewModel.lastrResult = res
            updateUiOnRes.forEach { action ->
                action(res)
            }
        }
    }

}