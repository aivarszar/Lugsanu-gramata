package com.convocatis.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.convocatis.app.database.AppDatabase
import com.convocatis.app.database.entity.ProfileEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class ConvocatisApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: SharedPreferences
        private set

    // Make gson a public property (remove private and getGson() function)
    val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
    }

    private var profileEntity: ProfileEntity? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        preferences = getSharedPreferences("convocatis_prefs", Context.MODE_PRIVATE)

        // Load profile from preferences
        loadProfile()
    }

    private fun loadProfile() {
        val profileJson = preferences.getString(PROFILE_KEY, null)
        profileEntity = if (profileJson != null) {
            gson.fromJson(profileJson, ProfileEntity::class.java)
        } else {
            ProfileEntity()
        }
    }

    fun getProfile(): ProfileEntity {
        if (profileEntity == null) {
            loadProfile()
        }
        return profileEntity ?: ProfileEntity()
    }

    fun saveProfile(profile: ProfileEntity) {
        profileEntity = profile
        val profileJson = gson.toJson(profile)
        preferences.edit().putString(PROFILE_KEY, profileJson).apply()
    }

    fun clearProfile() {
        profileEntity = ProfileEntity()
        preferences.edit().remove(PROFILE_KEY).apply()
    }

    companion object {
        private const val PROFILE_KEY = "profile_entity"

        @Volatile
        private var instance: ConvocatisApplication? = null

        fun getInstance(): ConvocatisApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}
