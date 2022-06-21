package it.unibo.mobilesystems



import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import com.google.android.gms.location.FusedLocationProviderClient
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.geo.PathCalculator
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.OSRMRoadManager.MEAN_BY_FOOT
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline



private const val ACTIVITY_NAME = "RoadRouting Activity"
class RoutingExtensionActivity : MainMapsActivity(), MapEventsReceiver {


    protected lateinit var mLocationManager: LocationManager
    protected lateinit var locationService: FusedLocationProviderClient
    protected lateinit var pathCalculator: PathCalculator

    private lateinit var roadManager: RoadManager

    private var destinationPoint: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //mLocationManager =
        roadManager = OSRMRoadManager(this,"Luca&Luca" )

        //Initialized as going always on foot
        (roadManager as OSRMRoadManager).setMean(MEAN_BY_FOOT)

        val mapEventsOverlay = MapEventsOverlay(this)
        map.getOverlays().add(0, mapEventsOverlay);

        pathCalculator = PathCalculator(roadManager)
        pathCalculator.addOnPathCalculatedUiUpdate {
            addRoadPolyLineToMap(it)
            putNodeMarkers(it)
        }
    }

    override fun onLocationChanged(p0: Location) {
        super.onLocationChanged(p0)
        val ciao = 1
    }

    fun calculatePathToPoint(endPoint: GeoPoint){
        val startPoint = mLocationOverlay.myLocation
        if(startPoint != null){
            pathCalculator.requestPathCalculation(startPoint, endPoint)
        }else{
            Debugger.printDebug(ACTIVITY_NAME, "MyPosition is Null - Cannot calculate the RoadPath")
        }
    }

    private fun addRoadPolyLineToMap(road: Road){
        val roadOverlay = RoadManager.buildRoadOverlay(road)
        roadOverlay.id = "Luca"

        map.overlays.removeIf {
            if(it is Marker && it.id == "Luca")
                return@removeIf true
            if(it is Polyline && it.id == "Luca")
                return@removeIf true
            return@removeIf false
        }


        map.overlays.add(roadOverlay)
        map.invalidate()
    }

    private fun putNodeMarkers(road: Road){
        val nodeIcon = applicationContext.getDrawable(R.drawable.marker_node)
        for (i in 0 until road.mNodes.size) {
            val node: RoadNode = road.mNodes[i]
            val nodeMarker = Marker(map)
            nodeMarker.id = "Luca"
            nodeMarker.position = node.mLocation
            nodeMarker.icon = nodeIcon
            nodeMarker.title = "Step $i"

            //Fill Bubbles
            fillBubbles(nodeMarker, node)

            map.overlays.add(nodeMarker)
            map.invalidate()
        }
    }

    private fun fillBubbles(nodeMarker: Marker, node: RoadNode){
        nodeMarker.snippet = node.mInstructions
        nodeMarker.subDescription = Road.getLengthDurationText(this, node.mLength, node.mDuration)

        val icon = applicationContext.getDrawable(R.drawable.ic_continue)
        nodeMarker.image = icon
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        //TODO("Not yet implemented")
        Debugger.printDebug(ACTIVITY_NAME, "singletapConfirmedHelper(p: $p)")
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        return if(p != null){
            destinationPoint = p
            calculatePathToPoint(p)
            true
        }else
            false
    }
}