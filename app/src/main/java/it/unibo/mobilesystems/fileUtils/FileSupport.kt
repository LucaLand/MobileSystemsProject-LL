package it.unibo.mobilesystems.fileUtils

import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import it.unibo.kactor.MsgUtil
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.debugUtils.DebuggerContextNameAnnotation
import it.unibo.mobilesystems.msgUtils.RobotMove
import it.unibo.mobilesystems.msgUtils.RobotMsgUtils
import java.io.*
import java.util.*


object FileSupport {

    private const val dirName = "MobileSystemProj"

    fun readConfigurationMapFromFile(fileName: String, app: AppCompatActivity): MutableMap<String, String>{
        val mutableMap = mutableMapOf<String, String>()
        val bufferReaderFile = openAssetFileBuffer(fileName, app)
        try {
            bufferReaderFile?.use { r ->
                r.readLines().forEach {
                    val key = it.split(":")[0].trim()
                    val value = it.split(":")[1].trim()
                    Debugger.printDebug(
                        "readConfigurationFromFileMap",
                        "Key: $key || Value: $value"
                    )
                    mutableMap.set(key, value)
                }
            }
        } catch (e: FileNotFoundException) {
            Debugger.printDebug("FileNotFoundException: $fileName")
            //e.printStackTrace();
        } catch (e: IOException) {
            Debugger.printDebug("IOException: $fileName")
            //e.printStackTrace();
        }
        return mutableMap
    }


    fun readStringTranslationFileData(fileName: String, app: AppCompatActivity): MutableMap<String, RobotMove>{
        val mutableMap = mutableMapOf<String, RobotMove>()
        val bufferReaderFile = openAssetFileBuffer(fileName, app)
        try {
            bufferReaderFile?.use { r ->
                r.readLines().forEach {
                    if(!it.contains("//")) {
                        val key = it.split("-->")[0].trim()
                        val valueStr = it.split("-->")[1].trim()
                        val value = RobotMsgUtils.stringToRobotMove(valueStr)
                        Debugger.printDebug(
                            "readStringTranslationFileData",
                            "Key: $key || Value: $value"
                        )
                        if(value != null)
                            mutableMap[key] = value
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            Debugger.printDebug("FileNotFoundException: $fileName")
            throw e
            //e.printStackTrace();
        } catch (e: IOException) {
            Debugger.printDebug("IOException: $fileName")
            throw e
            //e.printStackTrace();
        }
        if(mutableMap.isEmpty())
            throw IOException("No Map in the File!")

        return mutableMap
    }






    @Throws(FileNotFoundException::class)
    private fun openAssetFileBuffer(fileName: String, app : AppCompatActivity): BufferedReader? {
        try {
            val r = BufferedReader(
                InputStreamReader(
                    app.assets.open(fileName)
                )
            )
            Debugger.printDebug("OPENING FILE: $fileName")
            return r
        } catch (e: FileNotFoundException) {
            //e.printStackTrace();
            //throw FileNotFoundException("File:$fileName not found!")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }



    private fun readConfigurationFromFile(fileName: String, app: AppCompatActivity): List<String>?{
        val bufferReaderFile = openAssetFileBuffer(fileName, app)
        try {
            bufferReaderFile?.use { r ->
                return r.readLines()
            }
        } catch (e: FileNotFoundException) {
            Debugger.printDebug("FileNotFoundException: $fileName")
            //e.printStackTrace();
        } catch (e: IOException) {
            Debugger.printDebug("IOException: $fileName")
            //e.printStackTrace();
        }
        return null
    }

    @DebuggerContextNameAnnotation("UUID Reading From File")
     fun getUUIDFromAssetFile (fileName: String, app: AppCompatActivity): UUID? {
         readConfigurationFromFile(fileName, app)?.forEach { s: String ->
             return UUID.fromString(s.split("UUID: ")[1].trim())
         }

         Debugger.printDebug("Null UUID !!")
         return null
    }


    fun loadStringFromStorageFile(fileName: String): String {
        val file = loadFileFromStorage(dirName, fileName)
        try {
            BufferedReader(InputStreamReader(FileInputStream(file))).use { r ->
                return r.readLine()
            }
        } catch (e: FileNotFoundException) {
            Debugger.printDebug("FileNotFoundException: $fileName")
            //e.printStackTrace();
        } catch (e: IOException) {
            Debugger.printDebug("IOException: $fileName")
            //e.printStackTrace();
        }
        return ""
    }

    fun saveStringOnStorageFile(fileName: String, str: String?) {
        val file = loadFileFromStorage(dirName, fileName)
        try {
            val myWriter = FileWriter(file)
            myWriter.write(str)
            myWriter.close()
            Debugger.printDebug("Successfully wrote to the file.")
        } catch (e: IOException) {
            Debugger.printDebug("An error occurred Writing on file: $fileName")
            //e.printStackTrace();
        }
    }

    private fun loadFileFromStorage(directoryName: String, fileName: String?): File {
        val root: String = Environment.getExternalStorageDirectory()
            .absolutePath + File.separator + directoryName
        val myDir = File(root)
        myDir.mkdirs()
        return File(myDir, fileName)
    }


}