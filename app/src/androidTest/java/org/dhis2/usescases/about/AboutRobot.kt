package org.dhis2.usescases.about

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.dhis2.R
import org.dhis2.common.BaseRobot

fun aboutRobot(aboutBody: AboutRobot.() -> Unit) {
    AboutRobot().apply {
        aboutBody()
    }
}

class AboutRobot : BaseRobot() {

    fun checkVersionNames(rampCaptureName: String, appName: String, sdkName: String) {
        onView(withId(R.id.rampCaptureVersion)).check(matches(withText(rampCaptureName)))
        onView(withId(R.id.aboutApp)).check(matches(withText(appName)))
        onView(withId(R.id.appSDK)).check(matches(withText(sdkName)))
    }
}
