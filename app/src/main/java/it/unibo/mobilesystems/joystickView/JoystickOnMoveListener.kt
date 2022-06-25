package it.unibo.mobilesystems.joystickView

import it.unibo.mobilesystems.bluetooth.MyBluetoothService
import it.unibo.mobilesystems.msgUtils.RobotMove
import it.unibo.mobilesystems.msgUtils.RobotMsgUtils


class JoystickOnMoveListener : JoystickView.OnMoveListener {

    private var moveState : RobotMove = RobotMove.HALT


    override fun onMove(angle: Int, strength: Int) {
        //Debugger.printDebug("Joystick - OnMove", "Angle: $angle || Strength: $strength")
        var nextMove : RobotMove = RobotMove.HALT
        if(strength >= 90) {
            nextMove = angleToMove(angle)
        }
        if (moveState != nextMove) {
            MyBluetoothService.sendMsg(RobotMsgUtils.cmdMsgFactory(nextMove))
            moveState = nextMove
        }
    }

    private fun angleToMove(angle: Int): RobotMove {
        return when (angle) {
            in 0..30, in 330..360 -> RobotMove.RIGHT
            in 60..120 -> RobotMove.FORWARD
            in 150..210 -> RobotMove.LEFT
            in 240..300 -> RobotMove.BACKWARD
            else -> RobotMove.HALT
        }
    }




}