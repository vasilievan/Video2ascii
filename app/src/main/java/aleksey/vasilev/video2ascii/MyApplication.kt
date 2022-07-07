package aleksey.vasilev.video2ascii

import android.app.Application
import com.google.android.material.color.DynamicColors

/** DynamicColors for Android 12+.
 * */

class MyApplication: Application() {
    override fun onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this)
        super.onCreate()
    }
}