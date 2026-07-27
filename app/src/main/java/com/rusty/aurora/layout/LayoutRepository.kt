package com.rusty.aurora.layout

interface LayoutRepository {
    /** Ordered, always non-empty (falls back to DEFAULT_TILE_LAYOUT if
     *  nothing has been persisted yet, or the persisted value is corrupt). */
    fun getLayout(): List<TileConfig>

    /** No-ops if [tiles] would leave zero visible tiles - the dashboard
     *  always needs at least one card to show, so this is rejected here
     *  rather than trusted to the caller. */
    fun setLayout(tiles: List<TileConfig>)
}
