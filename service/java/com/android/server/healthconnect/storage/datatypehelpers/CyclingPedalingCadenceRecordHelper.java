/*
 * Copyright (C) 2022 The Android Open Source Project
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
 */

package com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.REAL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorDouble;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import android.content.ContentValues;
import android.database.Cursor;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.CyclingPedalingCadenceRecordInternal;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for CyclingPedalingCadenceRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE)
public class CyclingPedalingCadenceRecordHelper
        extends SeriesRecordHelper<
                CyclingPedalingCadenceRecordInternal,
                CyclingPedalingCadenceRecordInternal.CyclingPedalingCadenceRecordSample> {
    public static final int NUM_LOCAL_COLUMNS = 1;
    private static final String TABLE_NAME = "CyclingPedalingCadenceRecordTable";
    private static final String SERIES_TABLE_NAME = "cycling_pedaling_cadence_record_table";
    private static final String REVOLUTIONS_PER_MINUTE_COLUMN_NAME = "revolutions_per_minute";
    private static final String EPOCH_MILLIS_COLUMN_NAME = "epoch_millis";

    @Override
    String getMainTableName() {
        return TABLE_NAME;
    }

    @Override
    List<Pair<String, String>> getSeriesRecordColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>(NUM_LOCAL_COLUMNS);
        columnInfo.add(new Pair<>(EPOCH_MILLIS_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(REVOLUTIONS_PER_MINUTE_COLUMN_NAME, REAL));
        return columnInfo;
    }

    @Override
    String getSeriesDataTableName() {
        return SERIES_TABLE_NAME;
    }

    /** Populates the {@code record} with values specific to datatype */
    @Override
    void populateSpecificValues(
            @NonNull Cursor seriesTableCursor, CyclingPedalingCadenceRecordInternal record) {
        List<CyclingPedalingCadenceRecordInternal.CyclingPedalingCadenceRecordSample>
                cyclingPedalingCadenceRecordSampleList = new ArrayList<>();
        String uuid = getCursorString(seriesTableCursor, UUID_COLUMN_NAME);
        do {
            cyclingPedalingCadenceRecordSampleList.add(
                    new CyclingPedalingCadenceRecordInternal.CyclingPedalingCadenceRecordSample(
                            getCursorDouble(seriesTableCursor, REVOLUTIONS_PER_MINUTE_COLUMN_NAME),
                            getCursorLong(seriesTableCursor, EPOCH_MILLIS_COLUMN_NAME)));
        } while (seriesTableCursor.moveToNext()
                && uuid.equals(getCursorString(seriesTableCursor, UUID_COLUMN_NAME)));
        seriesTableCursor.moveToPrevious();
        record.setSamples(cyclingPedalingCadenceRecordSampleList);
    }

    @Override
    void populateSampleTo(
            ContentValues contentValues,
            CyclingPedalingCadenceRecordInternal.CyclingPedalingCadenceRecordSample
                    cyclingPedalingCadenceRecord) {
        contentValues.put(
                REVOLUTIONS_PER_MINUTE_COLUMN_NAME,
                cyclingPedalingCadenceRecord.getRevolutionsPerMinute());
        contentValues.put(EPOCH_MILLIS_COLUMN_NAME, cyclingPedalingCadenceRecord.getEpochMillis());
    }
}