package com.rusty.aurora.notifications

interface DndRepository {
    /** False both when DND is genuinely off and when policy access hasn't
     *  been granted - there's no real distinction from the dashboard's
     *  point of view, since it can't be toggled either way. */
    fun isEnabled(): Boolean

    /** No-op if policy access isn't granted (see DndAccessUtil) - degrades
     *  the same way every other optional-permission feature here does,
     *  never throws or crashes the request. */
    fun setEnabled(enabled: Boolean)
}
