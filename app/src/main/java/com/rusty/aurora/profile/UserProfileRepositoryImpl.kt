package com.rusty.aurora.profile

import android.content.Context

/** SharedPreferences-backed, same pattern as LayoutRepositoryImpl. */
class UserProfileRepositoryImpl(context: Context) : UserProfileRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)?.takeIf { it.isNotBlank() }

    override fun setUserName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        prefs.edit().putString(KEY_USER_NAME, trimmed).apply()
    }

    private companion object {
        const val PREFS_NAME = "aurora_profile"
        const val KEY_USER_NAME = "user_name"
    }
}
