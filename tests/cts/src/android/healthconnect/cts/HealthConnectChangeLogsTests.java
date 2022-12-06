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

package android.healthconnect.cts;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.ChangeLogTokenRequest;
import android.healthconnect.ChangeLogsRequest;
import android.healthconnect.ChangeLogsResponse;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.StepsRecord;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectChangeLogsTests {
    @Test
    public void testGetChangeLogToken() throws InterruptedException {
        assertThat(TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build()))
                .isNotNull();
    }

    @Test
    public void testChangeLogs_insert_default() throws InterruptedException {
        String token = TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);

        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(testRecord.size());
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);
    }

    @Test
    public void testChangeLogs_insert_dataOrigin_filter_incorrect() throws InterruptedException {
        String token =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder().setPackageName("random").build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);

        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);
    }

    @Test
    public void testChangeLogs_insert_dataOrigin_filter_correct() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String token =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);

        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(testRecord.size());
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);
    }

    @Test
    public void testChangeLogs_insert_record_filter() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String token =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .addRecordType(StepsRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);

        List<Record> testRecord = Collections.singletonList(TestUtils.getStepsRecord());
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(1);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);
        testRecord = Collections.singletonList(TestUtils.getHeartRateRecord());
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(1);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);
    }

    @Test
    public void testChangeLogs_insertAndDelete_default() throws InterruptedException {
        String token = TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);

        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        TestUtils.deleteRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(testRecord.size());
    }

    @Test
    public void testChangeLogs_insertAndDelete_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String token =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder().setPackageName("random").build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);

        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        TestUtils.deleteRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);
    }

    @Test
    public void testChangeLogs_insertAndDelete_dataOrigin_filter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String token =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);

        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        TestUtils.deleteRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(testRecord.size());
    }

    @Test
    public void testChangeLogs_insertAndDelete_record_filter() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String token =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .addRecordType(StepsRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(0);

        List<Record> testRecord = Collections.singletonList(TestUtils.getStepsRecord());
        TestUtils.insertRecords(testRecord);
        TestUtils.deleteRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(1);
        testRecord = Collections.singletonList(TestUtils.getHeartRateRecord());
        TestUtils.insertRecords(testRecord);
        TestUtils.deleteRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedRecordIds().size()).isEqualTo(1);
    }

    @Test
    public void testChangeLogs_insert_default_withPageSize() throws InterruptedException {
        String token = TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build());
        ChangeLogsRequest changeLogsRequest =
                new ChangeLogsRequest.Builder(token).setPageSize(1).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);

        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(1);
    }

    @Test
    public void testChangeLogs_insert_default_withNextPageToken() throws InterruptedException {
        String token = TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build());
        ChangeLogsRequest changeLogsRequest =
                new ChangeLogsRequest.Builder(token).setPageSize(1).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.hasMorePages()).isFalse();

        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.hasMorePages()).isTrue();
        assertThat(response.getUpsertedRecords().size()).isEqualTo(1);
        ChangeLogsRequest nextChangeLogsRequest =
                new ChangeLogsRequest.Builder(response.getNextChangesToken())
                        .setPageSize(1)
                        .build();
        ChangeLogsResponse nextResponse = TestUtils.getChangeLogs(nextChangeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(1);
        assertThat(nextResponse.hasMorePages()).isTrue();
        assertThat(nextResponse.getNextChangesToken())
                .isGreaterThan(response.getNextChangesToken());
        nextChangeLogsRequest =
                new ChangeLogsRequest.Builder(nextResponse.getNextChangesToken()).build();
        nextResponse = TestUtils.getChangeLogs(nextChangeLogsRequest);
        assertThat(nextResponse.hasMorePages()).isFalse();
    }

    @Test
    public void testChangeLogs_insert_default_withSamePageToken() throws InterruptedException {
        String token = TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.hasMorePages()).isFalse();
        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        ChangeLogsResponse newResponse = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(newResponse.getUpsertedRecords().size()).isEqualTo(testRecord.size());
    }
}