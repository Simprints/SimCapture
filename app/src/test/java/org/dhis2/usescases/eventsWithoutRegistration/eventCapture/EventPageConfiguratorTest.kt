package org.dhis2.usescases.eventsWithoutRegistration.eventCapture

import org.dhis2.utils.customviews.navigationbar.NavigationPageConfigurator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EventPageConfiguratorTest {
    private val eventCaptureRepository: EventCaptureContract.EventCaptureRepository = mock()
    private val pageConfigurator: NavigationPageConfigurator =
        EventPageConfigurator(eventCaptureRepository, isPortrait = true)

    @Test
    fun `displayTableView should follow repository Simprints RAMP history table availability`() {
        whenever(eventCaptureRepository.hasSimprintsRampProgramStageHistoryTable()) doReturn true
        assertTrue(pageConfigurator.displayTableView())
    }

    @Test
    fun `displayTableView should be false when Simprints RAMP history table is not configured`() {
        whenever(eventCaptureRepository.hasSimprintsRampProgramStageHistoryTable()) doReturn false
        assertFalse(pageConfigurator.displayTableView())
    }

    @Test
    fun `displayNotes should be true when Simprints RAMP history table is not configured`() {
        whenever(eventCaptureRepository.hasSimprintsRampProgramStageHistoryTable()) doReturn false
        assertTrue(pageConfigurator.displayNotes())
    }

    @Test
    fun `displayNotes should be false when Simprints RAMP history table is configured`() {
        whenever(eventCaptureRepository.hasSimprintsRampProgramStageHistoryTable()) doReturn true
        assertFalse(pageConfigurator.displayNotes())
    }
}
