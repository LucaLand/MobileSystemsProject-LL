package it.unibo.mobilesystems.debugUtils

object Debugger {


    fun printDebug(name: String, msg: Any?) {
        val string = msg.toString()
        printDebug("[$name] - $string")
    }

    fun printDebug(msg: String) {
        println("LL-MobileSystemProj [DEBUGGER]: $msg")

    }


    //EXPERIMENTING
    fun printDebugWName(useThis: Any?, msg: String){
        if(useThis!=null)
            printDebug(useThis?.javaClass?.name.toString(), msg)
        else
            printDebug(msg)
    }

    /** DEPRECATED **/

    @Deprecated("DEPRECATED printDebug(Obj) - Use printDebug(String) or printDebug(name, msg)")
    private fun printDebug(method: Any?, msg: Any?) {
        val annot = method?.javaClass?.getAnnotation(DebuggerContextNameAnnotation::class.java)
        val string = msg.toString()

        if (annot == null)
            printDebug(string)
        else
            printDebug("[${annot.contextName}] - $string")
    }

    @Deprecated("DEPRECATED printDebug(Obj) - Use printDebug(String) or printDebug(name, msg)")
    fun printDebug(msg: Any?) {
        //AUTO ANNOTATION GETTING NOT WORKING
        val annot =
            object {}.javaClass.enclosingMethod?.getAnnotation(DebuggerContextNameAnnotation::class.java)
        val string = msg.toString()

        if (annot == null)
            printDebug(string)
        else
            printDebug("[${annot.contextName}] - $string")
    }
}