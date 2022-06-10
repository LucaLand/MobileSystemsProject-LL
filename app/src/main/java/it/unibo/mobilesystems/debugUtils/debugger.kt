package it.unibo.mobilesystems.debugUtils

object Debugger {

    fun printDebug(msg: Any?) {
        val annot =
            object {}.javaClass.enclosingMethod?.getAnnotation(DebuggerContextNameAnnotation::class.java)
        val string = msg.toString()
        printDebug(string)

        if (annot == null)
            printDebug(string)
        else
            printDebug("[${annot.contextName}] - $string")
    }

    fun printDebug(name: String, msg: Any?) {
        val string = msg.toString()
        printDebug("[$name] - $string")
    }

    private fun printDebug(msg: String) {
        println("LL-MobileSystemProj [DEBUGGER]: $msg")
    }
}