package it.unibo.mobilesystems.fileUtils

import androidx.appcompat.app.AppCompatActivity
import it.unibo.mobilesystems.CONFIG_FILE_NAME
import it.unibo.mobilesystems.debugUtils.Debugger

object ConfigManager {

    var app : AppCompatActivity? = null
    var configMap : MutableMap<String, String>? = null

    fun init(app: AppCompatActivity?){
        configMap = readConfigMap(app)
    }

    private fun readConfigMap(app: AppCompatActivity?) : MutableMap<String, String>?{
        if(app != null){
            return FileSupport.readConfigurationMapFromFile(CONFIG_FILE_NAME, app)
            Debugger.printDebug("CONFIG MANAGER INITIALIZED - Map = $configMap")
        }else{
            Debugger.printDebug("CONFIG MANAGER NOT INITIALIZED - Call the ConfigManager.init(app) again")
            return null
        }
    }

    fun getConfigString(key: String) : String?{
        val value = configMap?.get(key)
        Debugger.printDebug("getConfig", "Key: $key || Value: $value")
        return value
    }
}