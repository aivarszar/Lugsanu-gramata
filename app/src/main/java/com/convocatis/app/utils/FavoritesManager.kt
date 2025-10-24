package com.convocatis.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages favorite texts using SharedPreferences
 * Stores RIDs of favorite texts
 */
class FavoritesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "favorites"
        private const val KEY_FAVORITES = "favorite_rids"
        private const val KEY_DEFAULTS_INITIALIZED = "defaults_initialized"
        private const val ADVERTISEMENT_RID = 999999L
    }

    /**
     * Get all favorite RIDs
     */
    fun getFavorites(): Set<Long> {
        val favoritesString = prefs.getString(KEY_FAVORITES, "") ?: ""
        return if (favoritesString.isEmpty()) {
            emptySet()
        } else {
            favoritesString.split(",").mapNotNull { it.toLongOrNull() }.toSet()
        }
    }

    /**
     * Check if text is favorite
     */
    fun isFavorite(rid: Long): Boolean {
        return getFavorites().contains(rid)
    }

    /**
     * Toggle favorite status
     */
    fun toggleFavorite(rid: Long): Boolean {
        val favorites = getFavorites().toMutableSet()
        val isNowFavorite = if (favorites.contains(rid)) {
            favorites.remove(rid)
            false
        } else {
            favorites.add(rid)
            true
        }
        saveFavorites(favorites)
        return isNowFavorite
    }

    /**
     * Add to favorites
     */
    fun addFavorite(rid: Long) {
        val favorites = getFavorites().toMutableSet()
        favorites.add(rid)
        saveFavorites(favorites)
    }

    /**
     * Remove from favorites
     */
    fun removeFavorite(rid: Long) {
        val favorites = getFavorites().toMutableSet()
        favorites.remove(rid)
        saveFavorites(favorites)
    }

    private fun saveFavorites(favorites: Set<Long>) {
        val favoritesString = favorites.joinToString(",")
        prefs.edit().putString(KEY_FAVORITES, favoritesString).apply()
    }

    /**
     * Initialize default favorites (call once on first app launch)
     * Adds the advertisement/info entry (RID 999999) as a default favorite
     */
    fun initializeDefaultFavorites() {
        val defaultsInitialized = prefs.getBoolean(KEY_DEFAULTS_INITIALIZED, false)
        if (!defaultsInitialized) {
            // Add advertisement entry as default favorite
            addFavorite(ADVERTISEMENT_RID)

            // Mark defaults as initialized
            prefs.edit().putBoolean(KEY_DEFAULTS_INITIALIZED, true).apply()
        }
    }
}
