package it.unibo.mobilesystems



import android.content.Context
import android.graphics.Color
import android.location.Address
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.android.gms.location.FusedLocationProviderClient
import it.unibo.mobilesystems.bluetoothUtils.MyBluetoothService
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.geo.Geocoder
import it.unibo.mobilesystems.geo.GeocoderViewModel
import it.unibo.mobilesystems.geo.PathCalculator
import it.unibo.mobilesystems.geo.PathViewModel
import it.unibo.mobilesystems.msgUtils.InstructionsTranslator
import it.unibo.mobilesystems.msgUtils.RobotMsgUtils
import it.unibo.mobilesystems.utils.hideKeyboard
import org.osmdroid.bonuspack.routing.*
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.util.*


const val CMD_TRASLATOR_DATA_FILE_NAME = "traslation.dataset"



private const val API_KEY = "fe7f0195-208c-4692-84ff-f9e3ef1e8fcc"
private const val TAG = "RoadRouting Activity"
class RoutingExtensionActivity : MainMapsActivity(), MapEventsReceiver {



    protected lateinit var mLocationManager: LocationManager
    protected lateinit var locationService: FusedLocationProviderClient
    protected lateinit var pathCalculator: PathCalculator




    private lateinit var mCompassOverlay: CompassOverlay
    private lateinit var mRotatioGestureOverlay: RotationGestureOverlay

    private lateinit var roadManager: RoadManager
    private lateinit var pathViewModel: PathViewModel
    private lateinit var geocoder: Geocoder
    private lateinit var geocoderViewModel: GeocoderViewModel


    private var destinationPoint: GeoPoint? = null


    //UI Components
    lateinit var destinationEditText: AutoCompleteTextView
    lateinit var goButton: Button
    lateinit var searchButton : Button


    //Vars
    private var isNavigating: Boolean = false
    private var road : Road? = null

    private var destMarker : Marker? = null //To delete from the map when a new destination is selected


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        addCompassToMap()
        enableMapRotation()
        InstructionsTranslator.setAppCompactActivity(this)
        mLocationManager.requestLocationUpdates(mLocationProvider, 500, 0.5F, this)

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
            if(!isNavigating) {
                if(goOnDest()) {
                    isNavigating = true
                    hideKeyboard(applicationContext, it)
                    it.setBackgroundColor(Color.RED)
                    (it as Button).text = "Stop!"
                }
            }else{
                it.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                (it as Button).text = "Go!"
                removeOverlaysByID("Luca")
                isNavigating = false
            }
        }
        searchButton.setOnClickListener {
            it.isEnabled = false
            it.setBackgroundColor(Color.LTGRAY)
            geocoderViewModel.asyncGetPlacesFromLocation(destinationEditText.text.toString())
            hideKeyboard(applicationContext, it)
        }

        //Callback when search ends
        geocoderViewModel.addUpdateUiOnResult { result ->
            result.onSuccess { addressList ->
                searchButton.isEnabled = true
                if(addressList.isNotEmpty()) {
                    destinationEditText.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, addressList.map {
                        var str = ""
                        for(i in 0..it.maxAddressLineIndex){
                            str +="${it.getAddressLine(i).trim()}, "
                        }
                        return@map str
                    }))

                    //Show Search List
                    destinationEditText.showDropDown()
                    Debugger.printDebug(TAG, "Finished Address Search!")

                }
            }
            result.onFailure {
                searchButton.isEnabled = false
                Debugger.printDebug(TAG, "Failed Address Search Request!")
            }
        }


        destinationEditText.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                //Set destination
                val addressList = geocoderViewModel.lastrResult.getOrThrow()
                selectedDestAddress(addressList[position])
                Debugger.printDebug(TAG, "Item Selected: [$position]")
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


         */

    }

    private fun enableMapRotation() {
        mRotatioGestureOverlay = RotationGestureOverlay(map)
        mRotatioGestureOverlay.isEnabled = true
        map.setMultiTouchControls(true)
        map.overlays.add(mRotatioGestureOverlay)
    }

    private fun selectedDestAddress(address: Address) {
        destinationEditText.setText(addressToString(address))
        val destPoint = GeoPoint(address.latitude, address.longitude)
        setDestinationPoint(destPoint)
    }

    private fun setDestinationPoint(destPoint: GeoPoint) {
        destinationPoint = destPoint
        addDestMarker(destPoint, "Destination")
        focusOnGeoPoint(destPoint, 20)
    }

    private fun focusOnGeoPoint(selectedPosition: GeoPoint, zoom: Int) {
        map.controller.zoomTo(zoom, 500)
        map.controller.animateTo(selectedPosition)
        Debugger.printDebug(TAG, "Point Focused (${selectedPosition.longitude}, ${selectedPosition.latitude})")
    }

    private fun addressToString(addr : Address): String {
        var str = ""
        for(i in 0 until addr.maxAddressLineIndex){
            str = str + addr.getAddressLine(i) +", "
        }
        return str + addr.getAddressLine(addr.maxAddressLineIndex)
    }

    private fun goOnDest(): Boolean {
       return if(destinationPoint != null && mLocationOverlay.myLocation != null) {
            calculatePathToPoint(destinationPoint!!)
            focusOnGeoPoint(mLocationOverlay.myLocation, 20)
            mLocationOverlay.enableFollowLocation()

            //Map orientation (not working)
            val dlat = mLocationOverlay.myLocation.latitude - destinationPoint!!.latitude
            val dlon = mLocationOverlay.myLocation.longitude - destinationPoint!!.longitude
            val angle = atan2(dlat, dlon).toFloat()
            map.setMapOrientation(angle, true)
            map.invalidate()
            true
        }
        else {
            Toast.makeText(this, "Insert Destination!! or Position is Null", 4).show()
            false
        }
    }

    override fun onLocationChanged(p0: Location) {
        super.onLocationChanged(p0)
        Debugger.printDebug(TAG, "Location Has Changed [$p0]")
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

                    //TEST Print of all Road Node instructions
                    var str = "Road:\n"
                    road.mNodes.forEach { node ->
                        str += "${node.mInstructions}\n"
                    }
                    Debugger.printDebug(TAG, str)


                    this.road = road
                }
                it.onFailure {
                    Toast.makeText(this, "AsyncRouting Error: ${it.localizedMessage}", 4).show()
                    this.road = null
                }

            }
        }else{
            Debugger.printDebug(TAG, "MyPosition is Null - Cannot calculate the RoadPath")
        }
    }

    private fun removeOverlaysByID(id: String){
        map.overlays.removeIf {
            if(it is Marker && it.id == id)
                return@removeIf true
            if(it is Polyline && it.id == id)
                return@removeIf true
            return@removeIf false
        }
    }

    private fun addRoadPolyLineToMap(road: Road){
        removeOverlaysByID("Luca")
        val roadOverlay = RoadManager.buildRoadOverlay(road)
        roadOverlay.id = "Luca"

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

    private fun addDestMarker(point: GeoPoint, title: String){
        val nodeIcon = applicationContext.getDrawable(R.drawable.marker_destination)
        val nodeMarker = Marker(map)
        nodeMarker.id = "Luca"
        nodeMarker.position = point
        nodeMarker.icon = nodeIcon
        nodeMarker.title = title

        map.overlays.remove(destMarker)
        map.overlays.add(nodeMarker)
        destMarker = nodeMarker
        map.invalidate()
    }

    private fun fillBubbles(nodeMarker: Marker, node: RoadNode){
        nodeMarker.snippet = node.mInstructions
        nodeMarker.subDescription = Road.getLengthDurationText(this, node.mLength, node.mDuration)

        //TEST
        val robotMove = InstructionsTranslator.deduceCommandByString(node.mInstructions)
        Debugger.printDebug(TAG, "Step: ${nodeMarker.title} || Instruction: ${node.mInstructions}")
        if(robotMove != null)
            MyBluetoothService.sendMsg(RobotMsgUtils.cmdMsgFactory(robotMove))

        val icon = applicationContext.getDrawable(R.drawable.ic_continue)
        nodeMarker.image = icon
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        //Debugger.printDebug(ACTIVITY_NAME, "singletapConfirmedHelper(p: $p)")
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        return if(p != null && !isNavigating){
            destinationPoint = p
            calculatePathToPoint(p)
            true
        }else
            false
    }

    private fun addCompassToMap(){
        mCompassOverlay = CompassOverlay(applicationContext, InternalCompassOrientationProvider(applicationContext), map)
        mCompassOverlay.enableCompass()
        map.overlays.add(mCompassOverlay)
    }
}