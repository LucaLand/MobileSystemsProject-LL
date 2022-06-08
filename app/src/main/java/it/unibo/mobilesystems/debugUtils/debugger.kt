package it.unibo.mobilesystems.debugUtils

object debugger {

    fun printDebug(msg : Any?){
        var string = msg.toString()
        printDebug(string)
    }
    private fun printDebug(msg : String){
        println("LL-MobileSystemProj [DEBUGGER]: $msg")
    }
}