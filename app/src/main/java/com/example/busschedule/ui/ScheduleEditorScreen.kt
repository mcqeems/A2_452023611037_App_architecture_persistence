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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.busschedule.R
import com.example.busschedule.data.BusSchedule
import com.example.busschedule.data.ScheduleRepository

/**
 * Editor screen used for both **Add** ([scheduleId] < 0) and **Edit**
 * ([scheduleId] points at an existing row). Persistence is delegated to the
 * [BusScheduleViewModel]; field-level validation is done with the pure
 * [ScheduleRepository.Rules.validate] function so the UI can render errors
 * without instantiating a repository.
 */
@Composable
fun ScheduleEditorScreen(
    scheduleId: Int,
    contentPadding: PaddingValues,
    onDone: () -> Unit,
    viewModel: BusScheduleViewModel = viewModel(factory = BusScheduleViewModel.factory),
) {
    val isEdit = scheduleId >= 0
    val existing: BusSchedule? = if (isEdit) {
        viewModel.observeSchedule(scheduleId).collectAsState(initial = null).value
    } else null

    var stopName by remember { mutableStateOf("") }
    var hourText by remember { mutableStateOf("") }
    var minuteText by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (existing != null && !touched) {
            stopName = existing.stopName
            val totalSeconds = existing.arrivalTimeInMillis
            hourText = (totalSeconds / 3600).toString()
            minuteText = ((totalSeconds % 3600) / 60).toString().padStart(2, '0')
        }
    }

    val hour = hourText.toIntOrNull() ?: -1
    val minute = minuteText.toIntOrNull() ?: -1
    val arrivalSeconds = if (hour in 0..23 && minute in 0..59) hour * 3600 + minute * 60 else -1
    val validation = remember(stopName, arrivalSeconds) {
        ScheduleRepository.validate(stopName, arrivalSeconds)
    }
    val fieldErrors = (validation as? ScheduleRepository.ValidationResult.Invalid)?.errors
        ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
    ) {
        Text(
            text = if (isEdit) stringResource(R.string.editor_title_edit)
            else stringResource(R.string.editor_title_add),
        )

        OutlinedTextField(
            value = stopName,
            onValueChange = {
                stopName = it
                touched = true
            },
            label = { Text(stringResource(R.string.stop_name)) },
            isError = ScheduleRepository.ScheduleFieldError.BlankStopName in fieldErrors,
            supportingText = {
                if (ScheduleRepository.ScheduleFieldError.BlankStopName in fieldErrors) {
                    Text(stringResource(R.string.error_blank_stop_name))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = hourText,
                onValueChange = {
                    hourText = it.filter(Char::isDigit).take(2)
                    touched = true
                },
                label = { Text(stringResource(R.string.field_hour)) },
                isError = ScheduleRepository.ScheduleFieldError.InvalidArrivalTime in fieldErrors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = minuteText,
                onValueChange = {
                    minuteText = it.filter(Char::isDigit).take(2)
                    touched = true
                },
                label = { Text(stringResource(R.string.field_minute)) },
                isError = ScheduleRepository.ScheduleFieldError.InvalidArrivalTime in fieldErrors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        if (ScheduleRepository.ScheduleFieldError.InvalidArrivalTime in fieldErrors) {
            Text(stringResource(R.string.error_invalid_time))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    touched = true
                    if (validation !is ScheduleRepository.ValidationResult.Valid) return@Button
                    if (isEdit && existing != null) {
                        viewModel.updateSchedule(
                            existing.copy(
                                stopName = stopName,
                                arrivalTimeInMillis = arrivalSeconds,
                            )
                        ) { success -> if (success) onDone() }
                    } else {
                        viewModel.addSchedule(stopName, arrivalSeconds) { success ->
                            if (success) onDone()
                        }
                    }
                },
                enabled = validation is ScheduleRepository.ValidationResult.Valid,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_save))
            }
            if (isEdit && existing != null) {
                IconButton(
                    onClick = {
                        viewModel.deleteSchedule(existing)
                        onDone()
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}