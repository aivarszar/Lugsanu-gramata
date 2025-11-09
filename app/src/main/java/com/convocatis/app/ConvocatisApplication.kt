package com.convocatis.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.convocatis.app.database.AppDatabase
import com.convocatis.app.utils.DataImporter
import com.convocatis.app.utils.FavoritesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ConvocatisApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: SharedPreferences
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        preferences = getSharedPreferences("convocatis_prefs", Context.MODE_PRIVATE)

        // Import initial data from XML files on first launch
        importInitialDataIfNeeded()

        // Initialize default favorites (adds advertisement entry)
        val favoritesManager = FavoritesManager(this)
        favoritesManager.initializeDefaultFavorites()
    }

    private fun importInitialDataIfNeeded() {
        val importer = DataImporter(this)
        if (!importer.isDataImported()) {
            Log.d(TAG, "First launch detected - importing initial data...")
            applicationScope.launch {
                try {
                    importer.importAllData()
                    Log.d(TAG, "Initial data import successful!")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to import initial data", e)
                }
            }
        } else {
            Log.d(TAG, "Data already imported, skipping...")
        }
    }

    companion object {
        private const val TAG = "ConvocatisApp"

        @Volatile
        private var instance: ConvocatisApplication? = null

        fun getInstance(): ConvocatisApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}
