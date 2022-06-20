package it.unibo.mobilesystems.joystickView

import it.unibo.kactor.MsgUtil
import it.unibo.mobilesystems.bluetoothUtils.MyBluetoothService
import it.unibo.mobilesystems.debugUtils.Debugger

    enum class RobotMove{
        FORWARD,
        BACKWARD,
        RIGHT,
        LEFT,
        HALT
    }


class JoystickOnMoveListener : JoystickView.OnMoveListener {

    private var moveState : RobotMove = RobotMove.HALT


    override fun onMove(angle: Int, strength: Int) {
        //Debugger.printDebug("Joystick - OnMove", "Angle: $angle || Strength: $strength")
        var nextMove : RobotMove = RobotMove.HALT
        if(strength >= 90) {
            nextMove = angleToMove(angle)
        }
        if (moveState != nextMove) {
            sendMsg(nextMove)
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

    private fun sendMsg(move: RobotMove){
        Debugger.printDebug("Joystick SendMsg", "Sending Move: $move")
        var msg = when(move){
            RobotMove.FORWARD -> {MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(w)", "basicrobot").toString()}
            RobotMove.RIGHT -> {MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(r)", "basicrobot").toString()}
            RobotMove.LEFT -> {MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(l)", "basicrobot").toString()}
            RobotMove.BACKWARD -> {MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(s)", "basicrobot").toString()}
            RobotMove.HALT -> {MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(h)", "basicrobot").toString()}
        }
        MyBluetoothService.sendMsg(msg)
    }


}