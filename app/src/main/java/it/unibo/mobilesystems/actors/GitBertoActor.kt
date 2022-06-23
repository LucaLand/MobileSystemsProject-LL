package it.unibo.mobilesystems.actors

import android.location.Location
import com.google.gson.Gson
import it.unibo.kactor.QActorBasicFsm
import it.unibo.kactor.annotations.*
import it.unibo.mobilesystems.bluetoothUtils.MyBluetoothService
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.geo.KLocation
import it.unibo.mobilesystems.msgUtils.InstructionsTranslator
import it.unibo.mobilesystems.msgUtils.RobotMove
import it.unibo.mobilesystems.msgUtils.RobotMsgUtils
import kotlinx.coroutines.delay
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint

const val ERROR_EVENT_NAME = "error"
const val ARRIVED_EVENT_NAME = "arrived"

const val GOAL_OK_DIST = 2
const val GIT_BERTO_ACTOR_NAME = "gitbertoactor"

@QActor(GIT_BERTO_CTX_NAME, GIT_BERTO_ACTOR_NAME)
class GitBertoActor() : QActorBasicFsm() {

    private val gson = Gson()
    private var road : Road? = null

    private lateinit var currentPosition : KLocation
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
        Debugger.printDebug(name, actorStringln("starting trip.."))
        Debugger.printDebug(name, actorStringln("received message $currentMsg"))
        road = gson.fromJson(currentMsg.msgContent().trim().removeFirstAndLast(), Road::class.java)
        Debugger.printDebug(name, actorStringln("obrained road: $road"))
        goals = ArrayDeque(road!!.mNodes)
    }

    @State
    @EpsilonMove("nextGoal2AnalyzeMove", "analyzeMove")
    suspend fun nextGoal() {
        val instructions = goals.first().mInstructions
        Debugger.printDebug(name, "next goal: $instructions")
        val moveType = InstructionsTranslator.deduceCommandByString(instructions)
        if(moveType == null) {
            Debugger.printDebug(name, "unable to translate instruction into move")
            emit event ERROR_EVENT_NAME withContent "cannot translate the next instruction: $instructions"
        } else {
            nextMoveType = moveType
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
    @WhenDispatch("followTheRoad2Work", "stopTrip", "stopTrip")
    suspend fun followTheRoad() {
        Debugger.printDebug(name, "following the road...")
        MyBluetoothService.sendMsg(RobotMsgUtils.cmdMsgFactory(RobotMove.FORWARD))
    }

    @State
    @EpsilonMove("turn2followTheRoad", "followTheRoad")
    suspend fun turn() {
        Debugger.printDebug(name, "turning $nextMoveType")
        MyBluetoothService.sendMsg(RobotMsgUtils.cmdMsgFactory(nextMoveType!!))
        delay(2500)
        MyBluetoothService.sendMsg(RobotMsgUtils.cmdMsgFactory(RobotMove.HALT))
    }

    @State
    @EpsilonMove("arrived2GoalDone", "stopTrip")
    suspend fun arrived() {
        Debugger.printDebug(name, "arrived")
        emit event ARRIVED_EVENT_NAME withContent "arrived"
    }

    @State
    @WhenEvent("checkAgain", "checkCurrentPosition", LOCATION_EVENT_NAME)
    @WhenDispatch("followTheRoad2Work", "stopTrip", "stopTrip")
    @EpsilonMove("checkCurrentPosition2GoalDone", "goalDone")
    suspend fun checkCurrentPosition() {
        Debugger.printDebug(name, actorStringln("currentMsg: $currentMsg"))
        currentPosition = gson.fromJson(currentMsg.msgContent().removeFirstAndLast(), KLocation::class.java)
        Debugger.printDebug(name, "current position: $currentPosition")
        val dist = GeoPoint(currentPosition.latitude, currentPosition.longitude)
            .distanceToAsDouble(goals.first().mLocation)
        Debugger.printDebug(name, "distance from next goal: $dist")
        if(dist < GOAL_OK_DIST) {
            Debugger.printDebug(name, actorStringln("robot is arrived to the position of the next goal"))
            goalDone = true
        } else {
            Debugger.printDebug(name, actorStringln("robot is far from the position of the next goal"))
        }
    }

    @GuardFor("checkCurrentPosition2GoalDone")
    fun checkGoalDone() : Boolean {
        return goalDone
    }

    @State
    @EpsilonMove("goalDone2NextMove", "nextGoal")
    suspend fun goalDone() {
        Debugger.printDebug(name, "goal done")
        MyBluetoothService.sendMsg(RobotMsgUtils.cmdMsgFactory(RobotMove.HALT))
        goals.removeFirst()
        goalDone = false
    }

    @State
    @EpsilonMove("stopTrip2Work", "work")
    suspend fun stopTrip() {
        Debugger.printDebug(name, actorStringln("stopping trip"))
        MyBluetoothService.sendMsg(RobotMsgUtils.cmdMsgFactory(RobotMove.HALT))
        goals.clear()
        nextMoveType = RobotMove.HALT
    }

}