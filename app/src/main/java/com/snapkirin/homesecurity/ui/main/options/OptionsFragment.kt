package com.snapkirin.homesecurity.ui.main.options

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.util.UserAccount
import com.snapkirin.homesecurity.ui.main.MainActivity

class OptionsFragment : PreferenceFragmentCompat() {

    private lateinit var notificationPreference: Preference
    private lateinit var resetPasswordPreference: Preference
    private lateinit var logoutPreference: Preference

    private lateinit var parentActivity: MainActivity

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_options, rootKey)

        parentActivity = requireActivity() as MainActivity

        notificationPreference = findPreference("set_notification")!!
        resetPasswordPreference = findPreference("reset_password")!!
        logoutPreference = findPreference("logout")!!

        notificationPreference.setOnPreferenceClickListener {
            parentActivity.startNotificationSettings()
            true
        }
        resetPasswordPreference.setOnPreferenceClickListener {
            parentActivity.startResetPasswordActivity()
            true
        }
        logoutPreference.setOnPreferenceClickListener {
            UserAccount.clearUserFromSharedPref(requireContext())
            parentActivity.startLoginActivity()
            true
        }
    }

}