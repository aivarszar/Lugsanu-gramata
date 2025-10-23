package com.convocatis.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.convocatis.app.database.AppDatabase

class ConvocatisApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: SharedPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        preferences = getSharedPreferences("convocatis_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        @Volatile
        private var instance: ConvocatisApplication? = null

        fun getInstance(): ConvocatisApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}
