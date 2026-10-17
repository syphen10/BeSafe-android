package com.besafe.app

import android.app.Application
import android.content.Context
import com.besafe.app.data.SettingsRepository
import com.besafe.app.safety.AlertDispatcher
import com.besafe.app.safety.Notifications
import com.besafe.app.safety.SessionState
import kotlinx.coroutines.flow.MutableStateFlow

class BeSafeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}

/** Manual dependency container — small enough that a DI framework isn't worth it yet. */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val alerts = AlertDispatcher(context, settings)

    /** Written only by CheckInService (and the UI's "dismiss"); read by everyone. */
    val session = MutableStateFlow<SessionState>(SessionState.Idle)
}
