package org.dhis2.form.simprints.ramp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.resource.provideStringResource

const val FIXED_SECTION_NEXT_BUTTON_TEST_TAG = "FIXED_SECTION_NEXT_BUTTON"

@Composable
fun FixedSectionNextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.testTag(FIXED_SECTION_NEXT_BUTTON_TEST_TAG),
        style = ButtonStyle.TEXT,
        text = provideStringResource("action_next"),
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
            )
        },
        onClick = onClick,
    )
}
