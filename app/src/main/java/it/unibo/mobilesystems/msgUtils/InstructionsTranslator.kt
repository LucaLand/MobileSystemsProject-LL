package it.unibo.mobilesystems.msgUtils


import androidx.appcompat.app.AppCompatActivity
import it.unibo.mobilesystems.CMD_TRASLATOR_DATA_FILE_NAME
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.fileUtils.FileSupport.readStringTranslationFileData
import java.io.IOException


private const val TAG = "InstructionsTranslator"

object InstructionsTranslator {

    private var appCompatActivity: AppCompatActivity? = null

    private var translationMap: MutableMap<String, RobotMove>? = null

    /**
     *  PRIVATE FUNCTION
     *  **/

    private fun init(){
        if(appCompatActivity != null) {
            try {
                readStringTranslationFileData(CMD_TRASLATOR_DATA_FILE_NAME, appCompatActivity!!)
                /*  File Layout
                right --> r
                left --> l
                 */
            } catch (e: IOException) {
                translationMap = initDefaultMap()
                Debugger.printDebug(TAG, e.localizedMessage)
            }
        }else{
            Debugger.printDebug(TAG, "Activity not set! Set it with setAppCompactActivity()")
            translationMap = initDefaultMap()
        }
    }


    private fun initDefaultMap(): MutableMap<String, RobotMove> {
        val mutableMap = mutableMapOf<String, RobotMove>()
        mutableMap["Right"] = RobotMove.RIGHT
        mutableMap["Left"] = RobotMove.LEFT
        mutableMap["Continue"] = RobotMove.FORWARD
        mutableMap["???"] = RobotMove.BACKWARD
        mutableMap["destination"] = RobotMove.HALT
        mutableMap["Arrive"] = RobotMove.HALT

        Debugger.printDebug(TAG, "Initialized Default Translation Map")

        return mutableMap
    }


    /**
     * PUBLIC FUNCTION
     * **/

    fun deduceCommandByString(instruction: String): RobotMove?{
        if(translationMap == null)
            this.init()

        translationMap!!.forEach { entry ->
            if(instruction.contains(entry.key, true))
                return entry.value
        }
        Debugger.printDebug(TAG, "Cannot Translate Instruction!")
        return null
    }

    fun setAppCompactActivity(app: AppCompatActivity){
        this.appCompatActivity = app
    }



}