/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.android.healthconnect.controller.tests.dataentries.formatters.shared

import android.health.connect.internal.datatypes.utils.RecordMapper
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormattersIntegrationTest {
    @Test
    fun allRecordsHaveFormatters_exceptSkinTemperature() {
        val recordClasses =
            RecordMapper.getInstance().recordIdToExternalRecordClassMap.values.sortedBy { it.name }
        val supportedUIRecords =
            HealthPermissionToDatatypeMapper.getAllDataTypes().values.flatten().sortedBy { it.name }
        // TODO(b/320676565): Add formatter for SkinTemperatureRecord
        assertThat(recordClasses.filter{ record -> !record.name.equals("android.health.connect.datatypes.SkinTemperatureRecord")} ).isEqualTo(supportedUIRecords)
    }
}
