/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.busschedule.data

import kotlinx.coroutines.flow.Flow

/**
 * Domain layer for [BusSchedule] operations.
 *
 * Encapsulates the business rules that sit between the ViewModel and the
 * [BusScheduleDao] so that raw persistence concerns (ID generation, conflict
 * policies, query syntax) never leak into the UI layer.
 *
 * Business rules enforced here:
 *  - [stopName] must not be blank.
 *  - [arrivalTimeInMillis] must be a non-negative time-of-day expressed in
 *    seconds-since-midnight, i.e. in the inclusive range `[0, 86_400)`.
 *  - Insert auto-assigns the next available primary key.
 */
class ScheduleRepository(private val dao: BusScheduleDao) {

    /** Streams the schedule identified by [id] (or null if it has been deleted). */
    fun observeSchedule(id: Int): Flow<BusSchedule?> = dao.getById(id)

    /** Result of validating a [BusSchedule] before it reaches the DAO. */
    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val errors: List<ScheduleFieldError>) : ValidationResult
    }

    enum class ScheduleFieldError {
        BlankStopName,
        InvalidArrivalTime,
    }

    companion object Rules {
        const val SECONDS_PER_DAY = 24 * 60 * 60

        /**
         * Validates a candidate schedule. Pure function: no I/O, safe to call from
         * any layer (including composables) to gate UI state.
         */
        fun validate(stopName: String, arrivalTimeInMillis: Int): ValidationResult {
            val errors = buildList {
                if (stopName.isBlank()) add(ScheduleFieldError.BlankStopName)
                if (arrivalTimeInMillis !in 0 until SECONDS_PER_DAY) {
                    add(ScheduleFieldError.InvalidArrivalTime)
                }
            }
            return if (errors.isEmpty()) ValidationResult.Valid
            else ValidationResult.Invalid(errors)
        }
    }

    /**
     * Persists a new schedule. The DAO inserts with `OnConflictStrategy.IGNORE`,
     * so a duplicate key will be silently skipped — callers should treat the
     * returned id as the authoritative row that exists after the call.
     */
    suspend fun addSchedule(stopName: String, arrivalTimeInMillis: Int): Long {
        require(
            validate(stopName, arrivalTimeInMillis) is ValidationResult.Valid
        ) { "addSchedule called with invalid input" }
        val newId = dao.getMaxId() + 1
        val schedule = BusSchedule(
            id = newId,
            stopName = stopName.trim(),
            arrivalTimeInMillis = arrivalTimeInMillis,
        )
        dao.insert(schedule)
        return newId.toLong()
    }

    /** Updates an existing schedule. Throws if [schedule] does not pass [validate]. */
    suspend fun updateSchedule(schedule: BusSchedule) {
        require(
            validate(schedule.stopName, schedule.arrivalTimeInMillis) is ValidationResult.Valid
        ) { "updateSchedule called with invalid input" }
        dao.update(schedule.copy(stopName = schedule.stopName.trim()))
    }

    /** Removes a schedule row from the database. */
    suspend fun deleteSchedule(schedule: BusSchedule) {
        dao.delete(schedule)
    }
}