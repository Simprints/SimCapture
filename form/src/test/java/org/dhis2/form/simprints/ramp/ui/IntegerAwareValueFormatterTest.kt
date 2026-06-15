package org.dhis2.form.simprints.ramp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class IntegerAwareValueFormatterTest {
    @Test
    fun `getFormattedValue should use configured maximum decimal places`() {
        val formatter = IntegerAwareValueFormatter(maxDecimalPlaces = 1)

        assertEquals("9.6", formatter.getFormattedValue(9.56f))
    }

    @Test
    fun `getFormattedValue should use two decimal places by default`() {
        val formatter = IntegerAwareValueFormatter()

        assertEquals("9.57", formatter.getFormattedValue(9.567f))
    }

    @Test
    fun `getFormattedValue should not add decimals to integers`() {
        val formatter = IntegerAwareValueFormatter(maxDecimalPlaces = 1)

        assertEquals("9", formatter.getFormattedValue(9f))
    }
}
