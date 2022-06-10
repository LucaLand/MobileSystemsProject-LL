package it.unibo.mobilesystems

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import it.unibo.mobilesystems.bluetoothUtils.BluetoothTest
import it.unibo.mobilesystems.databinding.ActivityMapsBinding
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.fileUtils.FileSupport
import it.unibo.mobilesystems.permissionManager.PermissionType
import it.unibo.mobilesystems.permissionManager.PermissionsManager.permissionCheck
import it.unibo.mobilesystems.permissionManager.PermissionsManager.permissionsCheck
import it.unibo.mobilesystems.permissionManager.PermissionsManager.permissionsRequest
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*


class MapsActivity : AppCompatActivity(), LocationListener {

    //TODO: FIX Activity Opening and returning (Creates a new Activity every time)
    //TODO: CREATE A MOTION PAD TO SEND MESSAGE TO THE ROBOT


    private lateinit var binding: ActivityMapsBinding

    private lateinit var map : MapView
    lateinit var mLocationOverlay : MyLocationNewOverlay
    lateinit var locationProvider: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FileSupport.init(this)

        internetPermissions()
        locationPermission()
        setProvider()
        enableLocationOnDevice()
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        map = startMap()
    }
    @SuppressLint("MissingPermission")
    private fun startMap (): MapView {
        map = findViewById(R.id.mapview)
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.setMultiTouchControls(true)
        map.maxZoomLevel = 22.0
        map.minZoomLevel = 1.0

        mLocationOverlay = MyLocationNewOverlay(map)
        mLocationOverlay.enableMyLocation()
        map.overlays.add(this.mLocationOverlay)

        Debugger.printDebug("LocationProvider: $locationProvider ${mLocationOverlay.myLocationProvider}")

        //ITALY Center GeoPoint
        map.controller.setCenter(GeoPoint(42.820897, 12.532178))

        //MAP INITIALIZATION
        map.controller.zoomTo(8, 0)
        mLocationOverlay.enableFollowLocation()
        return map
    }


    private fun enableLocationOnDevice() {
    //CODE BY : https://stackoverflow.com/questions/43518520/how-to-ask-user-to-enable-gps-at-the-launch-of-application

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here.
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            // Cast to a resolvable exception.
                            val resolvable: ResolvableApiException = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                this, LocationRequest.PRIORITY_HIGH_ACCURACY
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.

                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                    }
                }
            }
        }
    }

    private fun internetPermissions(){
        val permissions = arrayOf(  Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE)

        if(!permissionsCheck(this, permissions))
            permissionsRequest(this, permissions, 4)
    }

    private fun locationPermission(){
        permissionCheck(PermissionType.Location, this)
    }

    private fun setProvider(){
        //Set Provider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationProvider = LocationManager.FUSED_PROVIDER
        }else{
            locationProvider = LocationManager.NETWORK_PROVIDER //locationManager.getBestProvider()
        }
    }


    override fun onLocationChanged(p0: Location) {
        Debugger.printDebug("Location changed [$p0]")
    }

    fun myPositionButton(v : View){
        val rnd = Random()
        v.setBackgroundColor(
            Color.valueOf(rnd.nextInt(256).toFloat(),
            rnd.nextInt(256).toFloat(), rnd.nextInt(256).toFloat()).toArgb())
        Debugger.printDebug("HELLO")

        if(map.zoomLevelDouble < 14)
            map.controller.zoomTo(14, 500)
        map.controller.animateTo(mLocationOverlay.myLocation)
        mLocationOverlay.enableFollowLocation()
    }

    fun bluetoothButton (view: View){
        lateinit var activityClass : Class<out AppCompatActivity>
        activityClass = BluetoothTest::class.java
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LocationRequest.PRIORITY_HIGH_ACCURACY -> {
                if (resultCode == Activity.RESULT_OK) {
                    Debugger.printDebug("Status: ","On")
                } else {
                    Log.e("Status: ","Off")
                }
            }
        }
    }
}