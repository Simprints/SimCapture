package org.dhis2.usescases.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.dhis2.usescases.BaseTest
import org.dhis2.common.filters.filterRobotCommon
import org.dhis2.usescases.login.loginRobot
import org.dhis2.usescases.searchte.robot.filterRobot
import org.dhis2.usescases.settings.settingsRobot
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainTest : BaseTest() {

    @get:Rule
    val rule = ActivityTestRule(MainActivity::class.java, false, false)

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun checkHomeScreenRecyclerviewHasElements() {
        startActivity()
        homeRobot {
            checkViewIsNotEmpty()
        }
    }

    @Test
    fun shouldNavigateToHomeWhenBackPressed() {
        setupCredentials()
        startActivity()

        homeRobot {
            clickOnNavigationDrawerMenu()
            clickOnSettings()
            pressBack()
            checkHomeIsDisplayed()
        }
    }

    @Test
    fun checkDateFilterSetInitialDateWhenOpenedAgain(){
        setupCredentials()
        setDatePicker()
        startActivity()

        homeRobot {
            openFilters()
        }

        filterRobotCommon {
            openFilterAtPosition(0)
            clickOnFromToDateOption()
            selectDate(2020,6,15)
            acceptDateSelected()
            selectDate(2020,11,7)
            acceptDateSelected()
            clickOnFromToDateOption()
            checkDate(2020,6,15)
            acceptDateSelected()
            checkDate(2020,11,7)
        }
    }

    @Test
    fun shouldApplyFilterInProgramThatDoesNotApplyInHome(){
        setupCredentials()
        startActivity()
        val programPosition = 3 // This could be any program

        homeRobot {
            openProgramByPosition(programPosition)
        }

        filterRobot {
            clickOnFilter()
            clickOnEnrollmentDateFilter()
            clickOnTodayEnrollmentDate()
        }

        homeRobot {
            pressBack()
        }
    }

    @Test
    @Ignore
    fun shouldShowDialogToDeleteAccount() {
        setupCredentials()
        startActivity()

        homeRobot {
            clickOnNavigationDrawerMenu()
            clickDeleteAccount()
        }

        settingsRobot {
            Thread.sleep(1000)
            clickOnAcceptDialog()
        }

        loginRobot {
            checkUsernameFieldIsClear()
            checkPasswordFieldIsClear()
        }
    }

    private fun startActivity() {
        rule.launchActivity(null)
    }
}
