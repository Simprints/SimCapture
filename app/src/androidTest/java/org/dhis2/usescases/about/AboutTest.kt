package org.dhis2.usescases.about

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.dhis2.bindings.buildInfo
import org.dhis2.bindings.rampCaptureBuildInfo
import org.dhis2.BuildConfig
import org.dhis2.R
import org.dhis2.usescases.BaseTest
import org.dhis2.usescases.main.MainActivity
import org.dhis2.usescases.main.homeRobot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutTest : BaseTest() {

    @get:Rule
    val rule = ActivityTestRule(MainActivity::class.java, false, false)

    @Test
    fun shouldCheckVersionsWhenOpenAboutScreen() {
        startActivity()
        val rampCaptureVersion = getRampCaptureVersionName()
        val appVersion = getAppVersionName()
        val sdkVersion = getSDKVersionName()

        homeRobot {
            clickOnNavigationDrawerMenu()
            clickAbout()
        }

        aboutRobot {
            checkVersionNames(rampCaptureVersion, appVersion, sdkVersion)
        }
    }

    private fun startActivity() {
        rule.launchActivity(null)
    }

    private fun getRampCaptureVersionName() =
        String.format(
            context.getString(R.string.ramp_capture_version),
            context.rampCaptureBuildInfo(),
        )

    private fun getAppVersionName() =
        String.format(context.getString(R.string.about_app), context.buildInfo())

    private fun getSDKVersionName() =
        String.format(context.getString(R.string.about_sdk), BuildConfig.SDK_VERSION)

}
