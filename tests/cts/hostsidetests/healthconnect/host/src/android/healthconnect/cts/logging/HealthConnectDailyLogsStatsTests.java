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

package android.healthconnect.cts.logging;

import static com.google.common.truth.Truth.assertThat;

import android.cts.statsdatom.lib.AtomTestUtils;
import android.cts.statsdatom.lib.ConfigUtils;
import android.cts.statsdatom.lib.DeviceUtils;
import android.cts.statsdatom.lib.ReportUtils;

import com.android.os.StatsLog;
import com.android.os.healthfitness.api.ApiExtensionAtoms;
import com.android.os.healthfitness.api.HealthConnectStorageStats;
import com.android.os.healthfitness.api.HealthConnectUsageStats;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.util.CommandStatus;
import com.android.tradefed.util.RunUtil;

import com.google.protobuf.ExtensionRegistry;

import java.util.List;

public class HealthConnectDailyLogsStatsTests extends DeviceTestCase implements IBuildReceiver {

    public static final String TEST_APP_PKG_NAME = "android.healthconnect.cts.testhelper";
    private static final int NUMBER_OF_RETRIES = 5;

    private IBuildInfo mCtsBuild;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertThat(mCtsBuild).isNotNull();
        ConfigUtils.removeConfig(getDevice());
        ReportUtils.clearReports(getDevice());
    }

    @Override
    protected void tearDown() throws Exception {
        ConfigUtils.removeConfig(getDevice());
        ReportUtils.clearReports(getDevice());
        super.tearDown();
    }

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = buildInfo;
    }

    public void testConnectedApps() throws Exception {

        List<StatsLog.EventMetricData> data =
                triggerDailyJob(uploadAtomConfigForUsageStatsAtom(null));
        assertThat(data.size()).isAtLeast(1);
        HealthConnectUsageStats atom =
                data.get(0).getAtom().getExtension(ApiExtensionAtoms.healthConnectUsageStats);
        assertThat(atom.getConnectedAppsCount()).isGreaterThan(0);
        assertThat(atom.getAvailableAppsCount()).isGreaterThan(0);
    }

    public void testDatabaseStats() throws Exception {

        List<StatsLog.EventMetricData> data =
                triggerDailyJob(
                        uploadAtomConfigForStorageStatsAtom("testHealthConnectDatabaseStats"));
        assertThat(data.size()).isAtLeast(1);
        HealthConnectStorageStats atom =
                data.get(0).getAtom().getExtension(ApiExtensionAtoms.healthConnectStorageStats);
        assertThat(atom.getDatabaseSize()).isGreaterThan(0);
        assertThat(atom.getInstantDataCount()).isGreaterThan(0);
        assertThat(atom.getIntervalDataCount()).isGreaterThan(0);
        assertThat(atom.getSeriesDataCount()).isGreaterThan(0);
        assertThat(atom.getChangelogCount()).isGreaterThan(0);
    }

    private ExtensionRegistry uploadAtomConfigAndTriggerTest(String testName, int atomFieldNumber)
            throws Exception {
        ConfigUtils.uploadConfigForPushedAtoms(
                getDevice(), DeviceUtils.STATSD_ATOM_TEST_PKG, new int[] {atomFieldNumber});

        if (testName != null) {
            DeviceUtils.runDeviceTests(getDevice(), TEST_APP_PKG_NAME, ".LoggingTests", testName);
        }

        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        ApiExtensionAtoms.registerAllExtensions(registry);
        return registry;
    }

    private ExtensionRegistry uploadAtomConfigForUsageStatsAtom(String testName) throws Exception {
        return uploadAtomConfigAndTriggerTest(
                testName, ApiExtensionAtoms.HEALTH_CONNECT_USAGE_STATS_FIELD_NUMBER);
    }

    private ExtensionRegistry uploadAtomConfigForStorageStatsAtom(String testName)
            throws Exception {
        return uploadAtomConfigAndTriggerTest(
                testName, ApiExtensionAtoms.HEALTH_CONNECT_STORAGE_STATS_FIELD_NUMBER);
    }

    private List<StatsLog.EventMetricData> triggerDailyJob(ExtensionRegistry registry)
            throws Exception {

        // There are multiple instances of HealthConnectDailyService. This command finds the one
        // that needs to be triggered for this test using the job param 'hc_daily_job'.
        String output =
                getDevice()
                        .executeShellCommand(
                                "dumpsys jobscheduler | grep -m1 -A0 -B10 \"hc_daily_job\"");
        int indexOfStart = output.indexOf("/") + 1;
        String jobId = output.substring(indexOfStart, output.indexOf(":", indexOfStart));
        String jobExecutionCommand =
                "cmd jobscheduler run --namespace HEALTH_CONNECT_DAILY_JOB -f android " + jobId;

        executeLoggingJob(jobExecutionCommand, NUMBER_OF_RETRIES);
        RunUtil.getDefault().sleep(AtomTestUtils.WAIT_TIME_LONG);
        return ReportUtils.getEventMetricDataList(getDevice(), registry);
    }

    private void executeLoggingJob(String jobExecutionCommand, int retry)
            throws DeviceNotAvailableException, RuntimeException {
        if (retry == 0) {
            throw new RuntimeException("Could not execute job");
        }
        if (getDevice().executeShellV2Command(jobExecutionCommand).getStatus()
                != CommandStatus.SUCCESS) {
            executeLoggingJob(jobExecutionCommand, retry - 1);
        }
    }
}
