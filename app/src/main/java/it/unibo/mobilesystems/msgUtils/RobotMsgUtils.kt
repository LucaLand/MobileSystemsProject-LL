package it.unibo.mobilesystems.msgUtils


import it.unibo.mobilesystems.debugUtils.Debugger
import unibo.actor22comm.utils.CommUtils

object RobotMsgUtils {
    fun cmdMsgFactory(move: RobotMove): String{
        Debugger.printDebug("Joystick SendMsg", "Sending Move: $move")
        val cmdMove = when(move){
            RobotMove.FORWARD -> "w"
            RobotMove.RIGHT -> "r"
            RobotMove.LEFT -> "l"
            RobotMove.BACKWARD -> "s"
            RobotMove.HALT -> "h"
        }
        val msg = CommUtils.buildDispatch("BeautifulViewActivity","cmd", "cmd($cmdMove)", "basicrobot").toString()
        return "$msg\n"
    }

    fun stringToRobotMove(cmd: String): RobotMove?{
        return when(cmd){
            "w" -> RobotMove.FORWARD
            "r" -> RobotMove.RIGHT
            "l" -> RobotMove.LEFT
            "s" -> RobotMove.BACKWARD
            "h" -> RobotMove.HALT
            else -> {null}
        }
    }
}