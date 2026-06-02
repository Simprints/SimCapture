package org.dhis2.usescases.eventsWithoutRegistration.eventCapture

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.dhis2.form.model.EventMode
import org.dhis2.simprints.ramp.ui.EventHistoryTableFragment
import org.dhis2.usescases.eventsWithoutRegistration.eventCapture.eventCaptureFragment.EventCaptureFormFragment
import org.dhis2.usescases.notes.NotesFragment.Companion.newEventInstance
import org.dhis2.usescases.teiDashboard.dashboardfragments.indicators.IndicatorsFragment
import org.dhis2.usescases.teiDashboard.dashboardfragments.indicators.VISUALIZATION_TYPE
import org.dhis2.usescases.teiDashboard.dashboardfragments.indicators.VisualizationType
import org.dhis2.usescases.teiDashboard.dashboardfragments.relationships.RelationshipFragment
import org.dhis2.usescases.teiDashboard.dashboardfragments.relationships.RelationshipFragment.Companion.withArguments
import org.dhis2.utils.customviews.navigationbar.NavigationPage

class EventCapturePagerAdapter(
    private val fragmentActivity: FragmentActivity,
    private val programUid: String,
    private val eventUid: String,
    displayAnalyticScreen: Boolean,
    displaySimprintsRampHistoryTableScreen: Boolean,
    displayRelationshipScreen: Boolean,
    private val shouldOpenErrorSection: Boolean,
    private val eventMode: EventMode,
) : FragmentStateAdapter(fragmentActivity) {
    private val landscapePages: MutableList<EventPageType> = ArrayList()
    private val portraitPages: MutableList<EventPageType> = ArrayList()

    fun isFormScreenShown(currentItem: Int?): Boolean =
        currentItem != null &&
            currentItem in activePages.indices &&
            activePages[currentItem] == EventPageType.DATA_ENTRY

    fun isAnalyticsScreenShown(currentItem: Int?): Boolean =
        currentItem != null &&
            currentItem in activePages.indices &&
            activePages[currentItem] == EventPageType.ANALYTICS

    private enum class EventPageType {
        DATA_ENTRY,
        SIMPRINTS_RAMP_HISTORY_TABLE,
        ANALYTICS,
        RELATIONSHIPS,
        NOTES,
    }

    init {

        portraitPages.add(EventPageType.DATA_ENTRY)

        if (displayAnalyticScreen) {
            portraitPages.add(EventPageType.ANALYTICS)
            landscapePages.add(EventPageType.ANALYTICS)
        }

        if (displayRelationshipScreen) {
            portraitPages.add(EventPageType.RELATIONSHIPS)
            landscapePages.add(EventPageType.RELATIONSHIPS)
        }
        portraitPages.add(EventPageType.NOTES)
        landscapePages.add(EventPageType.NOTES)

        if (displaySimprintsRampHistoryTableScreen) {
            portraitPages.add(EventPageType.SIMPRINTS_RAMP_HISTORY_TABLE)
            landscapePages.add(EventPageType.SIMPRINTS_RAMP_HISTORY_TABLE)
        }
    }

    override fun createFragment(position: Int): Fragment =
        createFragmentForPage(
            if (isPortrait) portraitPages[position] else landscapePages[position],
        )

    private fun createFragmentForPage(pageType: EventPageType): Fragment =
        when (pageType) {
            EventPageType.ANALYTICS -> {
                val indicatorFragment: Fragment = IndicatorsFragment()
                val arguments = Bundle()
                arguments.putString(VISUALIZATION_TYPE, VisualizationType.EVENTS.name)
                indicatorFragment.arguments = arguments
                indicatorFragment
            }

            EventPageType.RELATIONSHIPS -> {
                val relationshipFragment: Fragment = RelationshipFragment()
                relationshipFragment.arguments =
                    withArguments(
                        programUid,
                        null,
                        null,
                        eventUid,
                    )
                relationshipFragment
            }

            EventPageType.NOTES -> {
                newEventInstance(programUid, eventUid)
            }

            EventPageType.SIMPRINTS_RAMP_HISTORY_TABLE -> {
                EventHistoryTableFragment.newInstance(eventUid)
            }

            EventPageType.DATA_ENTRY -> {
                EventCaptureFormFragment.newInstance(
                    eventUid,
                    shouldOpenErrorSection,
                    eventMode,
                )
            }
        }

    fun getNavigationPage(position: Int): NavigationPage? =
        activePages
            .getOrNull(position)
            ?.toNavigationPage()

    fun defaultLandscapeNavigationPage(): NavigationPage? =
        landscapePages
            .firstOrNull { it != EventPageType.SIMPRINTS_RAMP_HISTORY_TABLE }
            ?.toNavigationPage()
            ?: landscapePages.firstOrNull()?.toNavigationPage()

    fun getDynamicTabIndex(navigationPage: NavigationPage?): Int {
        val pageType =
            when (navigationPage) {
                NavigationPage.DATA_ENTRY -> EventPageType.DATA_ENTRY
                NavigationPage.TABLE_VIEW -> EventPageType.SIMPRINTS_RAMP_HISTORY_TABLE
                NavigationPage.ANALYTICS -> EventPageType.ANALYTICS
                NavigationPage.RELATIONSHIPS -> EventPageType.RELATIONSHIPS
                NavigationPage.NOTES -> EventPageType.NOTES
                else -> null
            }

        return if (pageType != null) {
            if (isPortrait) {
                portraitPages.indexOf(pageType)
            } else {
                landscapePages.indexOf(pageType)
            }
        } else {
            NO_POSITION
        }
    }

    private fun EventPageType.toNavigationPage(): NavigationPage =
        when (this) {
            EventPageType.DATA_ENTRY -> NavigationPage.DATA_ENTRY
            EventPageType.SIMPRINTS_RAMP_HISTORY_TABLE -> NavigationPage.TABLE_VIEW
            EventPageType.ANALYTICS -> NavigationPage.ANALYTICS
            EventPageType.RELATIONSHIPS -> NavigationPage.RELATIONSHIPS
            EventPageType.NOTES -> NavigationPage.NOTES
        }

    override fun getItemCount(): Int =
        if (isPortrait) {
            portraitPages.size
        } else {
            landscapePages.size
        }

    val isPortrait: Boolean
        get() = fragmentActivity.resources.configuration.orientation == 1

    private val activePages: List<EventPageType>
        get() = if (isPortrait) portraitPages else landscapePages

    companion object {
        const val NO_POSITION: Int = -1
    }
}
