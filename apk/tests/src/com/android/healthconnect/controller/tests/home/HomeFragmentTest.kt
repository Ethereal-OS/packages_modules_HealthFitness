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
package com.android.healthconnect.controller.tests.home

import android.content.Context
import android.health.connect.HealthDataCategory
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.home.HomeFragment
import com.android.healthconnect.controller.home.HomeFragmentViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel.MigrationFragmentState.WithData
import com.android.healthconnect.controller.migration.api.MigrationState
import com.android.healthconnect.controller.recentaccess.RecentAccessEntry
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel.RecentAccessState
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TestTimeSource
import com.android.healthconnect.controller.tests.utils.di.FakeFeatureUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.tests.utils.whenever
import com.android.healthconnect.controller.utils.FeatureUtils
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class HomeFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    private lateinit var context: Context

    @BindValue
    val homeFragmentViewModel: HomeFragmentViewModel =
        Mockito.mock(HomeFragmentViewModel::class.java)

    @BindValue
    val recentAccessViewModel: RecentAccessViewModel =
        Mockito.mock(RecentAccessViewModel::class.java)

    @BindValue
    val migrationViewModel: MigrationViewModel = Mockito.mock(MigrationViewModel::class.java)

    @BindValue val timeSource = TestTimeSource

    @Inject lateinit var fakeFeatureUtils: FeatureUtils
    private lateinit var navHostController: TestNavHostController
    private lateinit var idlingResource: CountingIdlingResource

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(WithData(MigrationState.IDLE))
        }
        (fakeFeatureUtils as FakeFeatureUtils).setIsNewAppPriorityEnabled(false)
        navHostController = TestNavHostController(context)

        // disable animations
        toggleAnimation(false)
    }

    @After
    fun teardown() {
        timeSource.reset()
        // enable animations
        toggleAnimation(true)
    }

    @Test
    fun appPermissions_navigatesToConnectedApps() {
        setupFragmentForNavigation()
        onView(withText("App permissions")).check(matches(isDisplayed()))
        onView(withText("App permissions")).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.connectedAppsFragment)
    }

    @Test
    fun dataAndAccess_navigatesToDataAndAccess() {
        setupFragmentForNavigation()
        onView(withText("Data and access")).check(matches(isDisplayed()))
        onView(withText("Data and access")).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.data_activity)
    }

    @Test
    fun seeAllRecentAccess_navigatesToRecentAccess() {
        setupFragmentForNavigation()
        onView(withText("See all recent access")).check(matches(isDisplayed()))
        onView(withText("See all recent access")).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.recentAccessFragment)
    }

    @Test
    fun recentAccessApp_navigatesToConnectedAppFragment() {
        setupFragmentForNavigation()
        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.connectedAppFragment)
    }

    @Test
    fun manageData_navigatesToManageData() {
        setupFragmentForNavigation()
        onView(withText("Manage data")).check(matches(isDisplayed()))
        onView(withText("Manage data")).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.manageDataFragment)
    }

    @Test
    fun test_HomeFragment_withRecentAccessApps() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle()),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle()))

        timeSource.setIs24Hour(true)

        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeFragmentViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle())

        onView(
                withText(
                    "Manage the health and fitness data on your phone, and control which apps can access it"))
            .check(matches(isDisplayed()))
        onView(withText("App permissions")).check(matches(isDisplayed()))
        onView(withText("None")).check(matches(isDisplayed()))
        onView(withText("Data and access")).check(matches(isDisplayed()))
        onView(withText("Manage data")).check(doesNotExist())

        onView(withText("Recent access")).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText("18:40")).check(matches(isDisplayed()))
        onView(withText("See all recent access")).check(matches(isDisplayed()))
    }

    @Test
    fun test_HomeFragment_withRecentAccessApps_in12HourFormat() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle()),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle()))

        timeSource.setIs24Hour(false)

        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeFragmentViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle())

        onView(withText("Recent access")).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText("6:40 PM")).check(matches(isDisplayed()))
        onView(withText("See all recent access")).check(matches(isDisplayed()))
    }

    @Test
    fun test_HomeFragment_withNoRecentAccessApps() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeFragmentViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED)))
        }
        launchFragment<HomeFragment>(Bundle())

        onView(
                withText(
                    "Manage the health and fitness data on your phone, and control which apps can access it"))
            .check(matches(isDisplayed()))
        onView(withText("App permissions")).check(matches(isDisplayed()))
        onView(withText("2 apps have access")).check(matches(isDisplayed()))
        onView(withText("Data and access")).check(matches(isDisplayed()))
        onView(withText("Manage data")).check(doesNotExist())

        onView(withText("Recent access")).check(matches(isDisplayed()))
        onView(withText("No apps recently accessed Health\u00A0Connect"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_HomeFragment_withOneApp() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeFragmentViewModel.connectedApps).then {
            MutableLiveData(listOf(ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)))
        }

        launchFragment<HomeFragment>(Bundle())

        onView(
                withText(
                    "Manage the health and fitness data on your phone, and control which apps can access it"))
            .check(matches(isDisplayed()))
        onView(withText("App permissions")).check(matches(isDisplayed()))
        onView(withText("1 app has access")).check(matches(isDisplayed()))
        onView(withText("Data and access")).check(matches(isDisplayed()))
        onView(withText("Manage data")).check(doesNotExist())
    }

    @Test
    fun test_HomeFragment_withOneAppConnected_oneAppNotConnected() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeFragmentViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)))
        }

        launchFragment<HomeFragment>(Bundle())

        onView(
                withText(
                    "Manage the health and fitness data on your phone, and control which apps can access it"))
            .check(matches(isDisplayed()))
        onView(withText("App permissions")).check(matches(isDisplayed()))
        onView(withText("1 of 2 apps have access")).check(matches(isDisplayed()))
        onView(withText("Data and access")).check(matches(isDisplayed()))
        onView(withText("Manage data")).check(doesNotExist())
    }

    @Test
    fun test_HomeFragment_withNewAppPriorityFlagOn() {
        (fakeFeatureUtils as FakeFeatureUtils).setIsNewAppPriorityEnabled(true)
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeFragmentViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED)))
        }
        launchFragment<HomeFragment>(Bundle())

        onView(
                withText(
                    "Manage the health and fitness data on your phone, and control which apps can access it"))
            .check(matches(isDisplayed()))
        onView(withText("App permissions")).check(matches(isDisplayed()))
        onView(withText("2 apps have access")).check(matches(isDisplayed()))
        onView(withText("Data and access")).check(matches(isDisplayed()))
        onView(withText("Manage data")).check(matches(isDisplayed()))

        onView(withText("Recent access")).check(matches(isDisplayed()))
        onView(withText("No apps recently accessed Health\u00A0Connect"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_HomeFragment_withNewInformationArchitectureFlagOn() {
        (fakeFeatureUtils as FakeFeatureUtils).setIsNewInformationArchitectureEnabled(true)
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeFragmentViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED)))
        }
        launchFragment<HomeFragment>(Bundle())

        onView(
                withText(
                    "Manage the health and fitness data on your phone, and control which apps can access it"))
            .check(matches(isDisplayed()))
        onView(withText("App permissions")).check(matches(isDisplayed()))
        onView(withText("2 apps have access")).check(matches(isDisplayed()))
        onView(withText("Data and access")).check(matches(isDisplayed()))
        onView(withText("Manage data")).check(matches(isDisplayed()))

        onView(withText("Recent access")).check(matches(isDisplayed()))
        onView(withText("No apps recently accessed Health\u00A0Connect"))
            .check(matches(isDisplayed()))
    }

    private fun setupFragmentForNavigation() {
        (fakeFeatureUtils as FakeFeatureUtils).setIsNewInformationArchitectureEnabled(true)
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle()),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle()))

        timeSource.setIs24Hour(true)

        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeFragmentViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
    }
}
