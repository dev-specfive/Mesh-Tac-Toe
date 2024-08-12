package com.specfive.meshtactoe.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.GsonBuilder
import com.specfive.meshtactoe.model.NodeInvite
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesHelperImpl @Inject constructor(val context: Context, prefFileName: String) :
    PreferencesHelper {

    //    private val masterKey: MasterKey =
//        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
//    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
//        context,
//        prefFileName,
//        masterKey,
//        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
//    )
    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)

    override fun clearPreferences() {
        prefs.edit().clear().apply()
    }

    override fun clearKey(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun putString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    override fun getString(key: String): String {
        return prefs.getString(key, "") ?: ""
    }

    override fun putLong(key: String, value: Long) {
        prefs.edit { putLong(key, value) }
    }

    override fun getLong(key: String): Long {
        return prefs.getLong(key, 0)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    override fun getBoolean(key: String): Boolean {
        return prefs.getBoolean(key, false)
    }

    private suspend fun <T> putObject(key: String, `object`: T) {
        val jsonString = GsonBuilder().create().toJson(`object`)
        prefs.edit { putString(key, jsonString) }
    }

    private inline fun <reified T> getObject(key: String): T? {
        val value = this.getString(key)
        return try {
            GsonBuilder().create().fromJson(value, T::class.java)
        } catch (e: Exception) {
            Timber.e("try_catch_error => $e")
            null
        }
    }

    private inline fun <reified T> getList(key: String): List<T>? {
        val value = this.getString(key)
        return GsonBuilder().create().fromJson<List<T>>(
            value, object : com.google.gson.reflect.TypeToken<List<T>>() {}.type
        )
    }

    fun saveInviteSentList() {

    }

    suspend fun saveList(node: List<NodeInvite>) {
        putObject("nodes_info", node)
    }

    fun getList(): List<NodeInvite> {
        return getList("nodes_info") ?: arrayListOf()
    }

}