package it.unibo.mobilesystems



import android.graphics.Color
import android.location.Address
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import it.unibo.kactor.ActorBasicFsm
import it.unibo.kactor.MsgUtil
import it.unibo.kactor.QakContext
import it.unibo.mobilesystems.actors.*
import it.unibo.mobilesystems.bluetooth.MyBluetoothService
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.geo.Geocoder
import it.unibo.mobilesystems.geo.GeocoderViewModel
import it.unibo.mobilesystems.geo.PathCalculator
import it.unibo.mobilesystems.geo.PathViewModel
import it.unibo.mobilesystems.msgUtils.InstructionsTranslator
import it.unibo.mobilesystems.msgUtils.RobotMsgUtils
import it.unibo.mobilesystems.utils.hideKeyboard
import kotlinx.coroutines.runBlocking
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
import kotlin.math.atan2


const val CMD_TRASLATOR_DATA_FILE_NAME = "traslation.dataset"



private const val API_KEY = "fe7f0195-208c-4692-84ff-f9e3ef1e8fcc"
private const val TAG = "RoadRouting Activity"
class RoutingExtensionActivity : MainMapsActivity(), MapEventsReceiver {

    protected lateinit var locationService: FusedLocationProviderClient
    protected lateinit var pathCalculator: PathCalculator




    private lateinit var mCompassOverlay: CompassOverlay
    private lateinit var mRotatioGestureOverlay: RotationGestureOverlay

    private lateinit var roadManager: RoadManager
    private lateinit var pathViewModel: PathViewModel
    private lateinit var geocoder: Geocoder
    private lateinit var geocoderViewModel: GeocoderViewModel


    private var destinationPoint: GeoPoint? = null

    //Actors
    private lateinit var gitBertoActor : ActorBasicFsm
    private lateinit var locationManagerActor : ActorBasicFsm

    private val gson = Gson()


    //UI Components
    lateinit var destinationEditText: AutoCompleteTextView
    lateinit var goButton: Button
    lateinit var searchButton : Button
    lateinit var pauseButton : FloatingActionButton


    //Vars
    private var isNavigating: Boolean = false
    private var road : Road? = null
    private var tripPaused = false

    private var destMarker : Marker? = null //To delete from the map when a new destination is selected


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addCompassToMap()
        enableMapRotation()
        InstructionsTranslator.setAppCompactActivity(this)
        //mLocationManager.requestLocationUpdates(mLocationProvider, 500, 0.5F, this)

        roadManager = GraphHopperRoadManager(API_KEY, false)

        //Initialized as going always on foot
        roadManager.addRequestOption("vehicle=foot")
        roadManager.addRequestOption("optimize=true")

        val mapEventsOverlay = MapEventsOverlay(this)
        map.overlays.add(0, mapEventsOverlay)

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
        pauseButton = findViewById(R.id.pauseButton)

        gitBertoActor = QakContext.getActor(GIT_BERTO_ACTOR_NAME) as ActorBasicFsm
        locationManagerActor = QakContext.getActor(LOCATION_MANAGER_ACTOR_NAME) as ActorBasicFsm

        goButton.setOnClickListener {
            if(!isNavigating) {
                //START NAVIGATION

                if(goOnDest()) {
                    isNavigating = true
                    tripPaused = false
                    hideKeyboard(applicationContext, it)
                    it.setBackgroundColor(Color.RED)
                    runBlocking {
                        MsgUtil.sendMsg(LMA_CMD_MESSAGE_NAME, ENABLE_LOCATION_MONITORING_ARG, locationManagerActor)
                        MsgUtil.sendMsg(GA_BEGIN_TRIP_MESSAGE_NAME, gson.toJson(road), gitBertoActor)
                    }
                    (it as Button).text = "Stop!"
                    pauseButton.isClickable = true
                    pauseButton.isVisible = true
                }
            }else{
                //STOP NAVIGATION

                it.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                (it as Button).text = "Go!"
                removeOverlaysByID("Luca")
                isNavigating = false
                runBlocking{
                    MsgUtil.sendMsg(GA_STOP_TRIP_MESSAGE_NAME, "stop", gitBertoActor)
                    MsgUtil.sendMsg(LMA_CMD_MESSAGE_NAME, DISABLE_LOCATION_MONITORING_ARG, locationManagerActor)
                }
                pauseButton.isClickable = false
                pauseButton.isVisible = false
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


        destinationEditText.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id -> //Set destination
                val addressList = geocoderViewModel.lastrResult.getOrThrow()
                selectedDestAddress(addressList[position])
                Debugger.printDebug(TAG, "Item Selected: [$position]")
                if(destinationPoint == null)
                    Toast.makeText(applicationContext, "No destination!", 3).show()
                else {
                    calculatePathToPoint(destinationPoint!!)
                    focusOnGeoPoint(destinationPoint!!, 16)
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
            //calculatePathToPoint(destinationPoint!!)
            focusOnGeoPoint(mLocationOverlay.myLocation, 19)
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

    /*override fun onLocationChanged(p0: Location) {
        super.onLocationChanged(p0)
        Debugger.printDebug(TAG, "Location Has Changed [$p0]")
        if(road != null){
            //TODO(NOT yet implemented!)
        }
    }*/

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

    fun pauseButton(view: View){
        view as FloatingActionButton
        if(isNavigating) {
            if(tripPaused) {
                //RESUME
                Debugger.printDebug(ACTIVITY_SERVICE, "resuming trip")
                runBlocking {
                    MsgUtil.sendMsg(LMA_CMD_MESSAGE_NAME, ENABLE_LOCATION_MONITORING_ARG, locationManagerActor)
                    MsgUtil.sendMsg(GA_RESUME_TRIP_MESSAGE_NAME, "", gitBertoActor)
                }
                view.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                //PAUSE
                Debugger.printDebug(ACTIVITY_SERVICE, "pausing trip")
                runBlocking {
                    MsgUtil.sendMsg(GA_PAUSE_TRIP_MESSAGE_NAME, "", gitBertoActor)
                    MsgUtil.sendMsg(LMA_CMD_MESSAGE_NAME, DISABLE_LOCATION_MONITORING_ARG, locationManagerActor)
                }
                view.setImageResource(android.R.drawable.ic_media_play)
            }
            tripPaused = !tripPaused
        }
    }
}