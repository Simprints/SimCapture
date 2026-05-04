package org.dhis2.usescases.searchTrackEntity.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dhis2.R
import org.hisp.dhis.mobile.ui.designsystem.component.ExtendedFAB
import org.hisp.dhis.mobile.ui.designsystem.component.FABStyle
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2TextStyle
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.hisp.dhis.mobile.ui.designsystem.theme.SurfaceColor
import org.hisp.dhis.mobile.ui.designsystem.theme.getTextStyle

@Composable
fun SimprintsBiometricSearchFallbackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier =
            modifier
                .fillMaxWidth()
                .requiredHeight(56.dp),
        onClick = onClick,
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = SurfaceColor.Primary,
            ),
        border = BorderStroke(1.dp, SurfaceColor.Primary),
        shape = RoundedCornerShape(Spacing.Spacing16),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_search),
            contentDescription = "",
            tint = SurfaceColor.Primary,
        )

        Spacer(modifier = Modifier.requiredWidth(Spacing.Spacing8))

        Text(
            text = stringResource(R.string.simprints_biometric_search_fallback_action),
            style = getTextStyle(style = DHIS2TextStyle.LABEL_LARGE),
        )
    }
}

@ExperimentalAnimationApi
@Composable
fun SimprintsNoneOfTheAboveButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ExtendedFAB(
        modifier = modifier,
        onClick = onClick,
        text = stringResource(R.string.simprints_none_of_the_above),
        icon = {},
        style = FABStyle.SECONDARY,
    )
}

