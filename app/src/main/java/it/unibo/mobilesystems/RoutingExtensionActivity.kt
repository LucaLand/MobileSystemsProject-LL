package it.unibo.mobilesystems



import android.location.Address
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.widget.doOnTextChanged
import com.google.android.gms.location.FusedLocationProviderClient
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.geo.Geocoder
import it.unibo.mobilesystems.geo.GeocoderViewModel
import it.unibo.mobilesystems.geo.PathCalculator
import it.unibo.mobilesystems.geo.PathViewModel
import it.unibo.mobilesystems.utils.hideKeyboard
import org.osmdroid.bonuspack.location.GeocoderGraphHopper
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.bonuspack.location.NominatimPOIProvider
import org.osmdroid.bonuspack.routing.*
import org.osmdroid.bonuspack.routing.OSRMRoadManager.MEAN_BY_FOOT
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.*


private const val API_KEY = "fe7f0195-208c-4692-84ff-f9e3ef1e8fcc"

private const val ACTIVITY_NAME = "RoadRouting Activity"
class RoutingExtensionActivity : MainMapsActivity(), MapEventsReceiver {



    protected lateinit var mLocationManager: LocationManager
    protected lateinit var locationService: FusedLocationProviderClient
    protected lateinit var pathCalculator: PathCalculator

    private var road : Road? = null

    private lateinit var roadManager: RoadManager
    private lateinit var pathViewModel: PathViewModel
    private lateinit var geocoder: Geocoder
    private lateinit var geocoderViewModel: GeocoderViewModel


    private var destinationPoint: GeoPoint? = null


    //UI Components
    lateinit var destinationEditText: AutoCompleteTextView
    lateinit var goButton: Button
    lateinit var searchButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //mLocationManager =
        roadManager = GraphHopperRoadManager(API_KEY, false)

        //Initialized as going always on foot
        roadManager.addRequestOption("vehicle=foot")
        roadManager.addRequestOption("optimize=true")

        val mapEventsOverlay = MapEventsOverlay(this)
        map.getOverlays().add(0, mapEventsOverlay)

        pathCalculator = PathCalculator(roadManager)
        pathViewModel = PathViewModel(pathCalculator)


        geocoder = Geocoder(android.location.Geocoder(this, Locale.getDefault()))
        //geocoder = Geocoder(GeocoderGraphHopper(Locale("it", "IT"), API_KEY))
        //geocoder = Geocoder(GeocoderNominatim( "Luca&Luca"))
        geocoderViewModel = GeocoderViewModel(geocoder)


        //UI Components
        destinationEditText = findViewById(R.id.destinationEditText)
        goButton = findViewById(R.id.destinationGoButton)
        searchButton = findViewById(R.id.destinationSearchButton)

        goButton.setOnClickListener {
            goOnDest()
            hideKeyboard(applicationContext, it)
        }
        searchButton.setOnClickListener {
            geocoderViewModel.asyncGetPlacesFromLocation(destinationEditText.text.toString())
            hideKeyboard(applicationContext, it)
        }



        geocoderViewModel.addUpdateUiOnResult { result ->
            result.onSuccess { addressList ->
                if(addressList.isNotEmpty()) {
                    destinationEditText.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, addressList.map {
                        var str = ""
                        for(i in 0..it.maxAddressLineIndex){
                            str +="${it.getAddressLine(i).trim()}, "
                        }
                        return@map str
                    }))
                    destinationEditText.showDropDown()
                    Debugger.printDebug(ACTIVITY_NAME, "Finished Address Search!")

                    destinationEditText.setText(addressToString(addressList[0]))
                    val destPoint = GeoPoint(addressList[0].latitude, addressList[0].longitude)
                    focusOnGeoPoint(destPoint)
                }
            }
        }
        /** AutoComplete text Code (some problems: -slow update; -Not working selecting item)**/
        /*
        geocoderViewModel.addUpdateUiOnResult { result ->
            result.onSuccess { addressList ->
                destinationEditText.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, addressList.map {
                    var str = ""
                    for(i in 0..it.maxAddressLineIndex){
                        str +="${it.getAddressLine(i).trim()}, "
                    }
                    return@map str
                }))
                //destinationEditText.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, addressList))
                destinationEditText.showDropDown()
                Debugger.printDebug(ACTIVITY_NAME, "Finished Address Search!")
                Debugger.printDebug(ACTIVITY_NAME, "${addressList.map {
                    var str = ""
                    for(i in 0..it.maxAddressLineIndex){
                        str +="${it.getAddressLine(i).trim()}, "
                    }
                    return@map str  }}")
                Toast.makeText(this, "Finished Search!", 2).show()
            }
        }



        destinationEditText.doOnTextChanged { text, start, before, count ->
            if(count > 6){
                Debugger.printDebug(ACTIVITY_NAME, "Starting AsyncSearch Location!")
                geocoderViewModel.asyncGetPlacesFromLocation(text.toString())
            }
        }

        destinationEditText.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                //destinationEditText.setText(destinationEditText.adapter.getItem(position).toString())
                val addr = geocoderViewModel.lastrResult.getOrThrow()[position]
                val selectedPosition = GeoPoint(addr.latitude, addr.longitude)
                map.controller.zoomTo(15, 500)
                map.controller.animateTo(selectedPosition)
                addMarker(selectedPosition, "Destination")
                Debugger.printDebug(ACTIVITY_NAME, "Address Selected (${selectedPosition.longitude}, ${selectedPosition.latitude})")
                this@RoutingExtensionActivity.destinationPoint = selectedPosition
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
         */

    }

    private fun focusOnGeoPoint(selectedPosition: GeoPoint) {

        map.controller.zoomTo(15, 500)
        map.controller.animateTo(selectedPosition)
        addMarker(selectedPosition, "Destination")
        Debugger.printDebug(ACTIVITY_NAME, "Address Selected (${selectedPosition.longitude}, ${selectedPosition.latitude})")
        this@RoutingExtensionActivity.destinationPoint = selectedPosition
    }

    private fun addressToString(addr : Address): String {
        var str = ""
        for(i in 0 until addr.maxAddressLineIndex){
            str = str + addr.getAddressLine(i) +", "
        }
        return str + addr.getAddressLine(addr.maxAddressLineIndex)
    }

    private fun goOnDest() {
        if(destinationPoint != null && mLocationOverlay.myLocation != null) {
            calculatePathToPoint(destinationPoint!!)
            map.controller.zoomTo(18, 400)
            mLocationOverlay.enableFollowLocation()
            mLocationOverlay.enableAutoStop = false
            val dlat = mLocationOverlay.myLocation.latitude - destinationPoint!!.latitude
            val dlon = mLocationOverlay.myLocation.longitude - destinationPoint!!.longitude
            val angle = atan2(dlat, dlon).toFloat()
            map.setMapOrientation(angle, true)
            map.invalidate()
        }
        else
            Toast.makeText(this, "Insert Destination!! or Position is Null", 4).show()
    }

    override fun onLocationChanged(p0: Location) {
        super.onLocationChanged(p0)
        if(road != null){
            //TODO(NOT yet implemented!)
        }
    }

    fun calculatePathToPoint(endPoint: GeoPoint){
        val startPoint = mLocationOverlay.myLocation
        if(startPoint != null){
            pathViewModel.asyncCalculatePath(startPoint, endPoint){
                it.onSuccess { road ->
                    addRoadPolyLineToMap(road)
                    addNodeMarkers(road)
                    var str = "Road:\n"
                    road.mNodes.forEach { node ->
                        str += "${node.mInstructions}\n"
                    }
                    Debugger.printDebug(ACTIVITY_NAME, str)
                    this.road = road
                }
                it.onFailure {
                    Toast.makeText(this, "AsyncRouting Error: ${it.localizedMessage}", 4).show()
                    this.road = null
                }

            }
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

    private fun addNodeMarkers(road: Road){
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

    private fun addMarker(point: GeoPoint, title: String){
        val nodeIcon = applicationContext.getDrawable(R.drawable.marker_node)
        val nodeMarker = Marker(map)
        nodeMarker.id = "Luca"
        nodeMarker.position = point
        nodeMarker.icon = nodeIcon
        nodeMarker.title = title

        map.overlays.add(nodeMarker)
        map.invalidate()
    }

    private fun fillBubbles(nodeMarker: Marker, node: RoadNode){
        nodeMarker.snippet = node.mInstructions
        nodeMarker.subDescription = Road.getLengthDurationText(this, node.mLength, node.mDuration)

        val icon = applicationContext.getDrawable(R.drawable.ic_continue)
        nodeMarker.image = icon
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        //Debugger.printDebug(ACTIVITY_NAME, "singletapConfirmedHelper(p: $p)")
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