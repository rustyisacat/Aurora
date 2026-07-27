package com.rusty.aurora.profile

interface UserProfileRepository {
    /** Null until the user has answered the first-launch name prompt. */
    fun getUserName(): String?

    fun setUserName(name: String)
}
