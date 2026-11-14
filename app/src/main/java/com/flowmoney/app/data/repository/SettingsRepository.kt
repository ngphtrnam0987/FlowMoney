package com.flowmoney.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flowmoney_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val KEY_DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val KEY_DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val KEY_ENABLED_BANKS = stringSetPreferencesKey("enabled_banks")
        val KEY_CURRENCY = stringPreferencesKey("currency")
    }

    val isOnboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    val isBiometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    val isDailyReminderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DAILY_REMINDER_ENABLED] ?: true
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DAILY_REMINDER_ENABLED] = enabled
        }
    }

    val dailyReminderTimeFlow: Flow<Pair<Int, Int>> = context.dataStore.data.map { preferences ->
        val hour = preferences[KEY_DAILY_REMINDER_HOUR] ?: 22
        val minute = preferences[KEY_DAILY_REMINDER_MINUTE] ?: 0
        Pair(hour, minute)
    }

    suspend fun setDailyReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DAILY_REMINDER_HOUR] = hour
            preferences[KEY_DAILY_REMINDER_MINUTE] = minute
        }
    }

    val enabledBanksFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_ENABLED_BANKS] ?: setOf(
            "com.VCB",
            "com.mbmobile",
            "vn.com.techcombank.bb.app",
            "com.vnpay.bidv",
            "com.vietinbank.ipay",
            "mobile.acb.com.vn",
            "com.tpb.mb.gprsandroid",
            "com.vpbank.neo",
            "com.vnpay.sacombank",
            "com.vib.myvib2"
        )
    }

    suspend fun setBankEnabled(bankPackage: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_ENABLED_BANKS]?.toMutableSet() ?: mutableSetOf(
                "com.VCB",
                "com.mbmobile",
                "vn.com.techcombank.bb.app",
                "com.vnpay.bidv",
                "com.vietinbank.ipay",
                "mobile.acb.com.vn",
                "com.tpb.mb.gprsandroid",
                "com.vpbank.neo",
                "com.vnpay.sacombank",
                "com.vib.myvib2"
            )
            if (enabled) {
                current.add(bankPackage)
            } else {
                current.remove(bankPackage)
            }
            preferences[KEY_ENABLED_BANKS] = current
        }
    }
}
