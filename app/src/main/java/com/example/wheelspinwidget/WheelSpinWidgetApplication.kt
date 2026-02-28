package com.example.wheelspinwidget

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bez.spinwheel_sdk.SpinWheelSdk
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WheelSpinWidgetApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        SpinWheelSdk.init(this)
    }

    override fun onTerminate() {
        SpinWheelSdk.destroy()
        super.onTerminate()
    }
}
