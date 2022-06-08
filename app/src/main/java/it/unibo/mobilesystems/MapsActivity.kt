package it.unibo.mobilesystems

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import it.unibo.mobilesystems.bluetoothUtils.BluetoothTest
import it.unibo.mobilesystems.databinding.ActivityMapsBinding
import it.unibo.mobilesystems.debugUtils.debugger
import it.unibo.mobilesystems.permissionManager.PermissionType
import it.unibo.mobilesystems.permissionManager.PermissionsManager.permissionCheck
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class MapsActivity : AppCompatActivity(), LocationListener {
    //TODO: FIX MAPS Activity (Maps not working)

    private lateinit var binding: ActivityMapsBinding

    lateinit var map : MapView
    lateinit var locationManager: LocationManager
    lateinit var mLocationOverlay : MyLocationNewOverlay


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

         binding = ActivityMapsBinding.inflate(layoutInflater)
         setContentView(binding.root)

        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        setContentView(R.layout.activity_maps)
        map = startMap()


    }

    @SuppressLint("MissingPermission")
    fun startMap (): MapView {
        val maps : MapView = findViewById(R.id.map)
        maps.setTileSource(TileSourceFactory.MAPNIK)
        maps.setMultiTouchControls(true)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


        //PERMISSION CHECK
        if(permissionCheck(PermissionType.Location, this)){
            locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 10.0f, this)
        }


        //MAP INITIALIZATION
        this.mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(applicationContext), maps)
        this.mLocationOverlay.enableMyLocation()
        maps.overlays.add(this.mLocationOverlay)

        mLocationOverlay.enableFollowLocation()
        maps.controller.zoomTo(14, 15)
        return maps
    }

    override fun onLocationChanged(p0: Location) {
        debugger.printDebug("Location changed")
        debugger.printDebug(p0)
    }

    fun hello(v : View){
        val rnd = Random()
        v.setBackgroundColor(
            Color.valueOf(rnd.nextInt(256).toFloat(),
            rnd.nextInt(256).toFloat(), rnd.nextInt(256).toFloat()).toArgb())
        println("HELLO")
        //map.controller.setCenter(mLocationOverlay.myLocation)
        map.controller.animateTo(mLocationOverlay.myLocation)
    }

    fun bluetoothButton (view: View){
        var rnd = kotlin.random.Random.nextInt()
        lateinit var activityClass : Class<out AppCompatActivity>

        /*if(rnd % 3 == 0){
            activityClass = BluetoothActivity::class.java
        }else{
            activityClass = MainActivity::class.java
        }
         */

        activityClass = BluetoothTest::class.java
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }


}