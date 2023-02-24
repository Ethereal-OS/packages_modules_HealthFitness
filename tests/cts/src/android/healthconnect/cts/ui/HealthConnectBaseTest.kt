package android.healthconnect.cts.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assume
import org.junit.Before

open class HealthConnectBaseTest {

    protected val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUpClass() {
        Assume.assumeTrue(isHardwareSupported())
    }

    private fun isHardwareSupported(): Boolean {
        // These UI tests are not optimised for Watches, TVs, Auto;
        // IoT devices do not have a UI to run these UI tests
        val pm: PackageManager = context.packageManager
        return (!pm.hasSystemFeature(PackageManager.FEATURE_EMBEDDED) &&
            !pm.hasSystemFeature(PackageManager.FEATURE_WATCH) &&
            !pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) &&
            !pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
    }
}