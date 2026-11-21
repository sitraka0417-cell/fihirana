package mg.sitraka.fihirana

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        UserStore.init(this)
        Audio.init(this)
        Downloads.init(this)
    }
}
