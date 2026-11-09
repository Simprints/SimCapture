package org.dhis2.form.simprints

import androidx.compose.runtime.compositionLocalOf
import org.dhis2.commons.simprints.usecases.SimprintsResolvePossibleDuplicatesSearchUseCase.SimprintsPossibleDuplicatesSearch

typealias SimprintsPossibleDuplicatesSearchHandler = (SimprintsPossibleDuplicatesSearch) -> Unit

val LocalSimprintsPossibleDuplicatesSearchHandler =
    compositionLocalOf<SimprintsPossibleDuplicatesSearchHandler?> { null }
