package it.unibo.mobilesystems.geo

import android.app.Application

class KLocationViewModel(application: Application) {

    val locationLiveData = KLocationLiveData(application)

}