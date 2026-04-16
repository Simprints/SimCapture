package org.dhis2.simprints

import android.content.Intent
import org.dhis2.form.ui.customintent.CustomIntentActivityResultContract
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SimprintsCustomIntentResultMapperTest {
    private val contract: CustomIntentActivityResultContract = mock()
    private val mapper = SimprintsCustomIntentResultMapper(contract)

    @Test
    fun `map should join returned values with commas`() {
        val responseData =
            listOf(
                CustomIntentResponseDataModel(
                    name = "guid",
                    extraType = CustomIntentResponseExtraType.STRING,
                    key = null,
                ),
            )
        val data: Intent = mock()
        whenever(contract.mapIntentResponseData(responseData, data)) doReturn
            listOf(
                "guid-1",
                "guid-2",
            )

        val result = mapper.map(responseData, data)

        assertEquals("guid-1,guid-2", result)
    }

    @Test
    fun `map should return null when contract returns null`() {
        val data: Intent = mock()
        whenever(contract.mapIntentResponseData(null, data)) doReturn null

        val result = mapper.map(responseData = null, data = data)

        assertNull(result)
    }

    @Test
    fun `map should return null when contract returns empty list`() {
        val data: Intent = mock()
        whenever(contract.mapIntentResponseData(null, data)) doReturn emptyList()

        val result = mapper.map(responseData = null, data = data)

        assertNull(result)
    }
}
