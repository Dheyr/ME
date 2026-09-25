package com.omnimail.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omnimail.app.model.*

object OmniStorage {
    private const val PREFS_NAME = "omnimail_storage_prefs"
    private const val KEY_ACCOUNTS = "key_email_accounts"
    private const val KEY_GROUPS = "key_workspace_groups"
    private const val KEY_EMAILS = "key_email_messages"
    private const val KEY_SETTINGS = "key_sync_settings"

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val defaultGroups = listOf(
        WorkspaceGroup(
            id = "group_personal",
            name = "Personal",
            colorHex = 0xFF2E7D32, // Forest Green
            accountIds = emptyList()
        ),
        WorkspaceGroup(
            id = "group_work",
            name = "Work & Business",
            colorHex = 0xFF6750A4, // Purple
            accountIds = emptyList()
        ),
        WorkspaceGroup(
            id = "group_support",
            name = "Customer Support",
            colorHex = 0xFF006D77, // Teal
            accountIds = emptyList()
        ),
        WorkspaceGroup(
            id = "group_marketing",
            name = "Cold Outreach / Campaign",
            colorHex = 0xFFD84A1B, // Burnt Orange
            accountIds = emptyList()
        )
    )

    fun loadAccounts(context: Context): List<EmailAccount> {
        val json = getPrefs(context).getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<EmailAccount>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAccounts(context: Context, accounts: List<EmailAccount>) {
        val json = gson.toJson(accounts)
        getPrefs(context).edit().putString(KEY_ACCOUNTS, json).apply()
    }

    fun loadGroups(context: Context): List<WorkspaceGroup> {
        val json = getPrefs(context).getString(KEY_GROUPS, null)
        if (json == null) {
            saveGroups(context, defaultGroups)
            return defaultGroups
        }
        return try {
            val type = object : TypeToken<List<WorkspaceGroup>>() {}.type
            gson.fromJson(json, type) ?: defaultGroups
        } catch (e: Exception) {
            defaultGroups
        }
    }

    fun saveGroups(context: Context, groups: List<WorkspaceGroup>) {
        val json = gson.toJson(groups)
        getPrefs(context).edit().putString(KEY_GROUPS, json).apply()
    }

    fun loadEmails(context: Context): List<EmailMessage> {
        val json = getPrefs(context).getString(KEY_EMAILS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<EmailMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveEmails(context: Context, emails: List<EmailMessage>) {
        val json = gson.toJson(emails)
        getPrefs(context).edit().putString(KEY_EMAILS, json).apply()
    }

    fun loadSettings(context: Context): SyncSettings {
        val json = getPrefs(context).getString(KEY_SETTINGS, null) ?: return SyncSettings()
        return try {
            gson.fromJson(json, SyncSettings::class.java) ?: SyncSettings()
        } catch (e: Exception) {
            SyncSettings()
        }
    }

    fun saveSettings(context: Context, settings: SyncSettings) {
        val json = gson.toJson(settings)
        getPrefs(context).edit().putString(KEY_SETTINGS, json).apply()
    }

    fun clearAllData(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
