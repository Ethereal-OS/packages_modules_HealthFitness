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
 */

package android.healthconnect.cts.backuprestore;

import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readRecords;
import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import static com.android.compatibility.common.util.BackupUtils.LOCAL_TRANSPORT_TOKEN;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.InstantRecord;
import android.health.connect.datatypes.IntervalRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Energy;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.platform.test.annotations.AppModeFull;
import android.provider.Settings;
import android.test.InstrumentationTestCase;

import androidx.test.InstrumentationRegistry;

import com.android.compatibility.common.util.BackupUtils;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AppModeFull
public class HealthConnectBackupRestoreCtsTest extends InstrumentationTestCase {
    /** Whether to print out logs in tests for debugging purposes. */
    private static final boolean IS_DEBUGGING_TEST = false;

    /** A permission that HC BR APK declares. This is used to find the package name of that APK. */
    private static final String HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION =
            "android.permission.HEALTH_CONNECT_BACKUP_INTER_AGENT";

    private static final int MAX_NUMBER_OF_RECORD_PER_INSERT_REQUEST = 500;

    private final BackupUtils mBackupUtils =
            new BackupUtils() {
                @Override
                protected InputStream executeShellCommand(String command) {
                    final ParcelFileDescriptor pfd =
                            getInstrumentation().getUiAutomation().executeShellCommand(command);
                    return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                }
            };
    private final Context mContext = InstrumentationRegistry.getTargetContext();

    private String mBackupRestoreApkPackageName;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mBackupRestoreApkPackageName = getBackupRestoreApkPackageName();
        // enable backup on the test device
        mBackupUtils.enableBackup(true);
        // switch backup transport to local
        mBackupUtils.setBackupTransportForUser(
                mBackupUtils.getLocalTransportName(), UserHandle.myUserId());
        // enable D2D backup flag so HealthConnectBackupAgent includes DB file in the backup file
        // list
        Settings.Secure.putString(
                mContext.getContentResolver(),
                "backup_local_transport_parameters",
                "is_device_transfer=true");

        deleteAllStagedRemoteData();
        verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
    }

    public void testBackupThenRestore_over2000Records_expectDataIsRestoredCorrectly()
            throws Exception {
        int numOfRecords = 2050;
        List<Record> insertedRecords =
                insertRecordsWithChunking(
                        this::getCompleteActiveCaloriesBurnedRecord, numOfRecords);
        assertThat(insertedRecords).hasSize(numOfRecords);

        mBackupUtils.backupNowAndAssertSuccess(mBackupRestoreApkPackageName);

        verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        readAndAssertRecordsNotExistUsingIds(insertedRecords);

        mBackupUtils.restoreAndAssertSuccess(LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName);

        readAndAssertRecordsExistUsingIds(insertedRecords);
    }

    private ActiveCaloriesBurnedRecord getCompleteActiveCaloriesBurnedRecord(long i) {
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setType(2)
                        .build();
        DataOrigin dataOrigin =
                // package name needs to match the caller's which is Context.getPackageName()
                new DataOrigin.Builder().setPackageName(mContext.getPackageName()).build();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        testMetadataBuilder.setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED);
        testMetadataBuilder.setClientRecordId("ClientRecordId" + UUID.randomUUID());

        Instant now = Instant.now();
        ZoneOffset zoneOffset = ZoneOffset.systemDefault().getRules().getOffset(now);
        return new ActiveCaloriesBurnedRecord.Builder(
                        testMetadataBuilder.build(),
                        now.minusMillis(i * 10),
                        now.minusMillis(i * 10 - 1000),
                        Energy.fromCalories(10.0))
                .setStartZoneOffset(zoneOffset)
                .setEndZoneOffset(zoneOffset)
                .build();
    }

    private static void readAndAssertRecordsExistUsingIds(List<Record> insertedRecords)
            throws InterruptedException {
        String logTag = "readAndAssertRecordsExistUsingIds";
        logRecords(logTag + " INPUT", insertedRecords);

        List<? extends Record> result = readRecordsUsingIds(insertedRecords);

        logRecords(logTag + " OUTPUT", result);
        assertThat(result).hasSize(insertedRecords.size());
        assertThat(result).containsExactlyElementsIn(insertedRecords);
    }

    private static void readAndAssertRecordsNotExistUsingIds(List<Record> insertedRecords)
            throws InterruptedException {
        String logTag = "readAndAssertRecordsNotExistUsingIds";
        logRecords(logTag + " INPUT", insertedRecords);

        List<? extends Record> result = readRecordsUsingIds(insertedRecords);

        logRecords(logTag + " OUTPUT", result);
        assertThat(result).isEmpty();
    }

    private static List<? extends Record> readRecordsUsingIds(List<Record> insertedRecords)
            throws InterruptedException {
        Class<? extends Record> clazz = insertedRecords.get(0).getClass();
        ReadRecordsRequestUsingIds.Builder<? extends Record> requestBuilder =
                new ReadRecordsRequestUsingIds.Builder<>(clazz);
        for (Record record : insertedRecords) {
            requestBuilder.addId(record.getMetadata().getId());
        }

        return readRecords(requestBuilder.build());
    }

    /**
     * Chunking is needed otherwise the insertion will fail with {@code
     * android.os.TransactionTooLargeException: data parcel size 1xxxxxx bytes}.
     */
    private List<Record> insertRecordsWithChunking(RecordCreator creator, int numOfRecords)
            throws InterruptedException {
        List<Record> insertedRecords = new ArrayList<>();

        for (int chunk = 0;
                chunk <= numOfRecords / MAX_NUMBER_OF_RECORD_PER_INSERT_REQUEST;
                chunk++) {
            List<Record> recordsToInsert = new ArrayList<>();

            for (int indexWithinChunk = 0;
                    indexWithinChunk < MAX_NUMBER_OF_RECORD_PER_INSERT_REQUEST;
                    indexWithinChunk++) {
                int index = chunk * MAX_NUMBER_OF_RECORD_PER_INSERT_REQUEST + indexWithinChunk;
                if (index >= numOfRecords) {
                    break;
                }

                recordsToInsert.add(creator.create(index));
            }
            if (!recordsToInsert.isEmpty()) {
                insertedRecords.addAll(insertRecords(recordsToInsert));
            }
        }

        return insertedRecords;
    }

    private interface RecordCreator {
        Record create(int index);
    }

    private String getBackupRestoreApkPackageName() {
        PackageManager packageManager = mContext.getPackageManager();
        List<PackageInfo> packageInfoList =
                packageManager.getInstalledPackages(
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        for (PackageInfo packageInfo : packageInfoList) {
            if (containsBackupRestoreInterAgentPermission(packageInfo.requestedPermissions)) {
                return packageInfo.packageName;
            }
        }
        throw new IllegalStateException("Backup Restore APK not found!");
    }

    private static boolean containsBackupRestoreInterAgentPermission(String[] array) {
        if (array == null) {
            return false;
        }
        for (String e : array) {
            if (HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION.equals(e)) {
                return true;
            }
        }
        return false;
    }

    private static void logRecords(String tag, List<? extends Record> records) {
        if (!IS_DEBUGGING_TEST) {
            return;
        }
        log("======================" + tag + "======================");
        for (int index = 0; index < records.size(); index++) {
            log("Record " + index + ":" + recordToString(records.get(index)));
        }
    }

    private static String recordToString(Record record) {
        StringBuilder stringBuilder = new StringBuilder();
        appendToLog(stringBuilder, 0, record.getClass().getSimpleName());

        if (record instanceof ActiveCaloriesBurnedRecord) {
            appendToLog(
                    stringBuilder,
                    1,
                    "Energy: ",
                    ((ActiveCaloriesBurnedRecord) record).getEnergy().getInCalories());
        }

        if (record instanceof IntervalRecord) {
            appendToLog(stringBuilder, 1, IntervalRecord.class.getSimpleName());
            appendToLog(stringBuilder, 2, "Start Time: ", ((IntervalRecord) record).getStartTime());
            appendToLog(stringBuilder, 2, "End Time: ", ((IntervalRecord) record).getEndTime());
        } else if (record instanceof InstantRecord) {
            appendToLog(stringBuilder, 1, InstantRecord.class.getSimpleName());
            appendToLog(stringBuilder, 2, "Time: ", ((InstantRecord) record).getTime());
        }

        Metadata metadata = record.getMetadata();
        appendToLog(stringBuilder, 1, "Metadata:");
        appendToLog(stringBuilder, 2, "getId: ", metadata.getId());
        appendToLog(stringBuilder, 2, "getClientRecordId: ", metadata.getClientRecordId());
        appendToLog(stringBuilder, 2, "device model: ", metadata.getDevice().getModel());
        appendToLog(stringBuilder, 2, "data origin: ", metadata.getDataOrigin().getPackageName());
        appendToLog(stringBuilder, 2, "getLastModifiedTime: ", metadata.getLastModifiedTime());

        return stringBuilder.toString();
    }

    private static void appendToLog(
            StringBuilder stringBuilder, int numberOfSpacePrefix, Object... values) {
        stringBuilder.append("\n");
        if (numberOfSpacePrefix > 0) {
            stringBuilder.append("-");
            stringBuilder.append(" ".repeat(numberOfSpacePrefix));
        }
        for (Object value : values) {
            stringBuilder.append(value);
        }
    }

    private static void log(String msg) {
        if (IS_DEBUGGING_TEST) {
            System.out.println(msg);
        }
    }
}
