package com.convocatis.app.utils

import android.content.Context
import android.util.Log
import com.convocatis.app.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports text data from XML files in assets/ to Room database
 */
class DataImporter(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val textDao = database.textDao()
    private val prefs = context.getSharedPreferences("convocatis_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "DataImporter"
        private const val KEY_DATA_IMPORTED = "data_imported_v4"  // v4: Fixed Rožukronis text sequence (2024-10-25)
    }

    /**
     * Check if initial data has been imported
     */
    fun isDataImported(): Boolean {
        return prefs.getBoolean(KEY_DATA_IMPORTED, false)
    }

    /**
     * Import all text data from XML files
     * Should be called on background thread
     */
    suspend fun importAllData() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting data import...")

            // Clear existing data
            textDao.deleteAllTexts()
            Log.d(TAG, "Cleared existing texts")

            // Import Latvian texts
            val latvianTexts = ImprovedXmlTextParser(context)
                .parseTextsFromAsset("conv_texts_lang_2.xml", "lv")
            textDao.insertAll(latvianTexts)
            Log.d(TAG, "Imported ${latvianTexts.size} Latvian texts")

            // Import English texts
            val englishTexts = ImprovedXmlTextParser(context)
                .parseTextsFromAsset("conv_texts_lang_34.xml", "en")
            textDao.insertAll(englishTexts)
            Log.d(TAG, "Imported ${englishTexts.size} English texts")

            // Mark as imported
            prefs.edit().putBoolean(KEY_DATA_IMPORTED, true).apply()
            Log.d(TAG, "Data import complete!")

        } catch (e: Exception) {
            Log.e(TAG, "Error importing data", e)
            throw e
        }
    }

    /**
     * Force re-import (clears flag)
     */
    fun clearImportFlag() {
        prefs.edit().putBoolean(KEY_DATA_IMPORTED, false).apply()
    }
}
