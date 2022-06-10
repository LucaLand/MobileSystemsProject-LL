package it.unibo.mobilesystems.fileUtils

import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import it.unibo.mobilesystems.bluetoothUtils.BluetoothTest
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.debugUtils.DebuggerContextNameAnnotation
import java.io.*
import java.util.*


object FileSupport {
    private const val dirName = "MobileSystemProj"
    //TODO (Read config file in a HashMap or Kotlin Pair)


    @Throws(FileNotFoundException::class)
    private fun openAssetFileBuffer(fileName: String, app : AppCompatActivity): BufferedReader? {
        try {
            val r = BufferedReader(
                InputStreamReader(
                    app.assets.open(fileName)
                )
            )
            Debugger.printDebug("OPENING FILE: $fileName")
            if (r == null) throw FileNotFoundException("File:$fileName not found!")
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