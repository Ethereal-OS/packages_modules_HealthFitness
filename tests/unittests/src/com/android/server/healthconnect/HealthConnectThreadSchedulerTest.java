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

package com.android.server.healthconnect;

import android.content.Context;
import android.os.Process;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.storage.TestUtils;

import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ThreadPoolExecutor;

public class HealthConnectThreadSchedulerTest {
    private ThreadPoolExecutor mInternalTaskScheduler;
    private ThreadPoolExecutor mControllerTaskScheduler;
    private ThreadPoolExecutor mForegroundTaskScheduler;
    private ThreadPoolExecutor mBackgroundTaskScheduler;
    private long mInternalTaskSchedulerCompletedJobs;
    private long mControllerTaskSchedulerCompletedJobs;
    private long mForegroundTaskSchedulerCompletedJobs;
    private long mBackgroundTaskSchedulerCompletedJobs;
    private Context mContext;

    @Before
    public void setUp() {
        mInternalTaskScheduler = HealthConnectThreadScheduler.sInternalBackgroundExecutor;
        mInternalTaskSchedulerCompletedJobs = mInternalTaskScheduler.getCompletedTaskCount();
        mControllerTaskScheduler = HealthConnectThreadScheduler.sControllerExecutor;
        mControllerTaskSchedulerCompletedJobs = mControllerTaskScheduler.getCompletedTaskCount();
        mForegroundTaskScheduler = HealthConnectThreadScheduler.sForegroundExecutor;
        mForegroundTaskSchedulerCompletedJobs = mForegroundTaskScheduler.getCompletedTaskCount();
        mBackgroundTaskScheduler = HealthConnectThreadScheduler.sBackgroundThreadExecutor;
        mBackgroundTaskSchedulerCompletedJobs = mBackgroundTaskScheduler.getCompletedTaskCount();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testHealthConnectSchedulerScheduleInternal() throws Exception {
        HealthConnectThreadScheduler.scheduleInternalTask(() -> {});
        TestUtils.waitForTaskToFinishSuccessfully(
                () -> {
                    if (mInternalTaskScheduler.getCompletedTaskCount()
                            != mInternalTaskSchedulerCompletedJobs + 1) {
                        throw new RuntimeException();
                    }
                });
        HealthConnectThreadScheduler.scheduleControllerTask(() -> {});
        TestUtils.waitForTaskToFinishSuccessfully(
                () -> {
                    if (mControllerTaskScheduler.getCompletedTaskCount()
                            != mControllerTaskSchedulerCompletedJobs + 1) {
                        throw new RuntimeException();
                    }
                });
        HealthConnectThreadScheduler.schedule(mContext, () -> {}, Process.myUid(), false);
        TestUtils.waitForTaskToFinishSuccessfully(
                () -> {
                    if (mBackgroundTaskScheduler.getCompletedTaskCount()
                            != mBackgroundTaskSchedulerCompletedJobs + 1) {
                        throw new RuntimeException();
                    }
                });
    }

    @Test
    public void testHealthConnectSchedulerClear() {
        HealthConnectThreadScheduler.resetThreadPools();
        setUp();
        Truth.assertThat(mInternalTaskSchedulerCompletedJobs).isEqualTo(0);
        Truth.assertThat(mControllerTaskSchedulerCompletedJobs).isEqualTo(0);
        Truth.assertThat(mForegroundTaskSchedulerCompletedJobs).isEqualTo(0);
        Truth.assertThat(mBackgroundTaskSchedulerCompletedJobs).isEqualTo(0);
    }
}
