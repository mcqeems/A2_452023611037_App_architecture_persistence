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
package com.example.busschedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.busschedule.BusScheduleApplication
import com.example.busschedule.data.BusSchedule
import com.example.busschedule.data.BusScheduleDao
import com.example.busschedule.data.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BusScheduleViewModel(
    private val busScheduleDao: BusScheduleDao,
    private val repository: ScheduleRepository,
) : ViewModel() {

    fun getFullSchedule(): Flow<List<BusSchedule>> = busScheduleDao.getAll()

    fun getScheduleFor(stopName: String): Flow<List<BusSchedule>> =
        busScheduleDao.getByStopName(stopName)

    fun observeSchedule(id: Int): Flow<BusSchedule?> = repository.observeSchedule(id)

    /** Saves a new schedule. Returns true on success, false on validation failure. */
    fun addSchedule(
        stopName: String,
        arrivalTimeInMillis: Int,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            val valid = ScheduleRepository.validate(stopName, arrivalTimeInMillis)
            if (valid !is ScheduleRepository.ValidationResult.Valid) {
                onResult(false)
                return@launch
            }
            runCatching { repository.addSchedule(stopName, arrivalTimeInMillis) }
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
    }

    /** Updates an existing schedule. Returns true on success. */
    fun updateSchedule(schedule: BusSchedule, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val valid = ScheduleRepository.validate(
                schedule.stopName,
                schedule.arrivalTimeInMillis,
            )
            if (valid !is ScheduleRepository.ValidationResult.Valid) {
                onResult(false)
                return@launch
            }
            runCatching { repository.updateSchedule(schedule) }
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
    }

    /** Deletes a schedule. */
    fun deleteSchedule(schedule: BusSchedule) {
        viewModelScope.launch { repository.deleteSchedule(schedule) }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as BusScheduleApplication)
                val dao = application.database.busScheduleDao()
                BusScheduleViewModel(dao, ScheduleRepository(dao))
            }
        }
    }
}