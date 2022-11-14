/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissiontypes

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.HealthDataCategoriesFragment.Companion.CATEGORY_NAME_KEY
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.categories.fromName
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Fragment for health permission types. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class HealthPermissionTypesFragment : Hilt_HealthPermissionTypesFragment() {

    companion object {
        private const val PERMISSION_TYPES = "permission_types"
        const val PERMISSION_TYPE_KEY = "permission_type_key"
        private const val DELETE_CATEGORY_DATA_BUTTON = "delete_category_data"
    }

    private lateinit var category: HealthDataCategory

    private val mPermissionTypes: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(PERMISSION_TYPES)
    }

    private val mDeleteCategoryData: Preference? by lazy {
        preferenceScreen.findPreference(DELETE_CATEGORY_DATA_BUTTON)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.health_permission_types_screen, rootKey)
        if (requireArguments().containsKey(CATEGORY_NAME_KEY) &&
            (requireArguments().getString(CATEGORY_NAME_KEY) != null)) {
            val categoryName = requireArguments().getString(CATEGORY_NAME_KEY)!!
            category = fromName(categoryName)
        }
        mPermissionTypes?.removeAll()
        // TODO(b/245513815): Only show permission types with data.
        category.healthPermissionTypes.forEach { permissionType ->
            mPermissionTypes?.addPreference(
                Preference(requireContext()).also {
                    it.setTitle(fromPermissionType(permissionType).label)
                    it.setOnPreferenceClickListener {
                        findNavController()
                            .navigate(
                                R.id.action_healthPermissionTypes_to_healthDataAccess,
                                getBundle(permissionType))
                        true
                    }
                })
        }
        mDeleteCategoryData?.title =
            getString(R.string.delete_category_data_button, getString(category.title))
        mDeleteCategoryData?.setOnPreferenceClickListener {
            // TODO(b/246161850) implement delete category data flow
            true
        }
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.permission_types_title)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    fun getBundle(permissionType: HealthPermissionType): Bundle {
        val bundle = Bundle()
        bundle.putSerializable(PERMISSION_TYPE_KEY, permissionType)
        return bundle
    }
}