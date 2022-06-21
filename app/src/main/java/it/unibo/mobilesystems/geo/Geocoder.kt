package it.unibo.mobilesystems.geo

import android.location.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.location.GeocoderGraphHopper
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.bonuspack.location.NominatimPOIProvider
import java.lang.Exception

class Geocoder(
    private val geocoderGraphHopper: GeocoderGraphHopper
) {

    private var job : Job? = null

    suspend fun getPlacesFromLocation(location : String) : Result<List<Address>> {
        if(job != null) {
            job!!.cancelAndJoin()
        }

        return withContext(Dispatchers.IO) {
            return@withContext try {
                Result.success(geocoderGraphHopper.getFromLocationName(location, 10))
            } catch (e : Exception) {
                Result.failure(e)
            }
        }
    }

}