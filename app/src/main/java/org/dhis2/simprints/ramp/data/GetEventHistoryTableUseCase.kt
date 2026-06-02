package org.dhis2.simprints.ramp.data

import org.dhis2.simprints.ramp.model.EventHistoryTable

class GetEventHistoryTableUseCase(
    private val repository: EventHistoryTableRepository,
) {
    operator fun invoke(): Result<EventHistoryTable?> =
        try {
            Result.success(repository.getTable())
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}
