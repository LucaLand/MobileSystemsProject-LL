package it.unibo.mobilesystems.actors

import android.location.Location
import com.google.gson.Gson
import it.unibo.kactor.QActorBasicFsm
import it.unibo.kactor.annotations.*
import it.unibo.mobilesystems.bluetoothUtils.MyBluetoothService
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.msgUtils.InstructionsTranslator
import it.unibo.mobilesystems.msgUtils.RobotMove
import it.unibo.mobilesystems.msgUtils.RobotMsgUtils
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint

const val ERROR_EVENT_NAME = "error"
const val ARRIVED_EVENT_NAME = "arrived"

const val GOAL_OK_DIST = 0.5

@QActor(GIT_BERTO_CTX_NAME)
class GitBertoActor() : QActorBasicFsm() {

    private val gson = Gson()
    private var road : Road? = null

    private lateinit var currentPosition : Location
    private var goals = ArrayDeque<RoadNode>()
    private var nextMoveType : RobotMove? = RobotMove.HALT
    private var goalDone = false

    @State
    @Initial
    @EpsilonMove("start2work", "work")
    suspend fun start() {
        Debugger.printDebug(name, actorStringln("started"))
    }

    @State
    @WhenDispatch("work2beginTrip", "beginTrip", "beginTrip")
    suspend fun work() {
        Debugger.printDebug(name, actorStringln("idle"))
    }

    @State
    @EpsilonMove("beginTrip2NextGoal", "nextGoal")
    suspend fun beginTrip() {
        road = gson.fromJson(currentMsg.msgContent(), Road::class.java)
        goals = ArrayDeque(road!!.mNodes)
    }

    @State
    @EpsilonMove("nextGoal2AnalyzeMove", "analyzeMove")
    suspend fun nextGoal() {
        val instructions = goals.first().mInstructions
        val moveType = InstructionsTranslator.deduceCommandByString(instructions)
        if(moveType == null) {
            emit event ERROR_EVENT_NAME withContent "cannot translate the next instruction: $instructions"
        }
    }

    @GuardFor("nextGoal2AnalyzeMove", "work")
    fun isMoveTranslated() : Boolean {
        return nextMoveType != null
    }

    @State
    @EpsilonMove("nextGoal2FollowTheRoad", "followTheRoad")
    @EpsilonMove("nextGoal2Turn", "turn")
    @EpsilonMove("nextGoal2Arrived", "arrived")
    suspend fun analyzeMove() { }

    @GuardFor("nextGoal2FollowTheRoad")
    fun isToFollowRoad() : Boolean {
        return nextMoveType == RobotMove.FORWARD
    }

    @GuardFor("nextGoal2Turn")
    fun isToTurn() : Boolean {
        return nextMoveType == RobotMove.RIGHT || nextMoveType == RobotMove.LEFT
    }

    @GuardFor("nextGoal2Arrived")
    fun isDestination() : Boolean {
        return nextMoveType == RobotMove.HALT
    }

    @State
    @WhenEvent("followTheRoad2CheckCurrentPosition", "checkCurrentPosition", LOCATION_EVENT_NAME)
    suspend fun followTheRoad() {
        MyBluetoothService.sendMsg(RobotMsgUtils.cmdMsgFactory(RobotMove.FORWARD))
    }

    @State
    @EpsilonMove("turn2followTheRoad", "followTheRoad")
    suspend fun turn() {

    }

    @State
    @EpsilonMove("arrived2GoalDone", "goalDone")
    suspend fun arrived() {
        emit event ARRIVED_EVENT_NAME withContent "arrived"
    }

    @State
    @WhenEvent("checkAgain", "checkCurrentPosition", LOCATION_EVENT_NAME)
    @EpsilonMove("checkCurrentPosition2GoalDone", "goalDone")
    suspend fun checkCurrentPosition() {
        currentPosition = gson.fromJson(currentMsg.msgContent(), Location::class.java)
        val dist = GeoPoint(currentPosition.latitude, currentPosition.longitude)
            .distanceToAsDouble(goals.first().mLocation)
        if(dist < GOAL_OK_DIST)
            goalDone = true
    }

    @GuardFor("checkCurrentPosition2GoalDone")
    fun checkGoalDone() : Boolean {
        return goalDone
    }

    @State
    @EpsilonMove("goalDone2NextMove", "nextMove")
    suspend fun goalDone() {
        goals.removeFirst()
        goalDone = false
    }

}