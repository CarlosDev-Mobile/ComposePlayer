package com.carlosdev.player.logic.audio.util

import android.content.Context
import android.content.SharedPreferences

class Prefs {
    private val _sharedPreferences: SharedPreferences

    private constructor(context: Context) {
        _sharedPreferences = context.applicationContext.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    }

    private constructor(context: Context, preferencesName: String) {
        _sharedPreferences = context.applicationContext.getSharedPreferences(
            preferencesName,
            Context.MODE_PRIVATE
        )
    }

    // String related methods
    fun readString(what: String?): String? {
        return _sharedPreferences.getString(what, DEFAULT_STRING_VALUE)
    }

    fun readString(what: String?, defaultString: String?): String? {
        return _sharedPreferences.getString(what, defaultString)
    }

    fun writeString(where: String?, what: String?) {
        _sharedPreferences.edit().putString(where, what).apply()
    }

    // int related methods
    fun readInt(what: String?): Int {
        return _sharedPreferences.getInt(what, DEFAULT_INT_VALUE)
    }

    fun readInt(what: String?, defaultInt: Int): Int {
        return _sharedPreferences.getInt(what, defaultInt)
    }

    fun writeInt(where: String?, what: Int) {
        _sharedPreferences.edit().putInt(where, what).apply()
    }

    // double related methods
    fun readDouble(what: String?): Double {
        if (!contains(what)) return DEFAULT_DOUBLE_VALUE
        return java.lang.Double.longBitsToDouble(readLong(what))
    }

    fun readDouble(what: String?, defaultDouble: Double): Double {
        if (!contains(what)) return defaultDouble
        return java.lang.Double.longBitsToDouble(readLong(what))
    }

    fun writeDouble(where: String?, what: Double) {
        writeLong(where, java.lang.Double.doubleToRawLongBits(what))
    }

    // float related methods
    fun readFloat(what: String?): Float {
        return _sharedPreferences.getFloat(what, DEFAULT_FLOAT_VALUE)
    }

    fun readFloat(what: String?, defaultFloat: Float): Float {
        return _sharedPreferences.getFloat(what, defaultFloat)
    }

    fun writeFloat(where: String?, what: Float) {
        _sharedPreferences.edit().putFloat(where, what).apply()
    }

    // long related methods
    fun readLong(what: String?): Long {
        return _sharedPreferences.getLong(what, DEFAULT_LONG_VALUE)
    }

    fun readLong(what: String?, defaultLong: Long): Long {
        return _sharedPreferences.getLong(what, defaultLong)
    }

    fun writeLong(where: String?, what: Long) {
        _sharedPreferences.edit().putLong(where, what).apply()
    }

    // boolean related methods
    @JvmOverloads
    fun readBoolean(what: String?, defaultBoolean: Boolean = DEFAULT_BOOLEAN_VALUE): Boolean {
        return _sharedPreferences.getBoolean(what, defaultBoolean)
    }

    fun writeBoolean(where: String?, what: Boolean) {
        _sharedPreferences.edit().putBoolean(where, what).apply()
    }

    // String set methods
    fun putStringSet(key: String?, value: Set<String?>?) {
        _sharedPreferences.edit().putStringSet(key, value).apply()
    }

    fun getStringSet(key: String?, defValue: Set<String?>?): Set<String>? {
        return _sharedPreferences.getStringSet(key, defValue)
    }

    // end related methods
    fun remove(key: String) {
        if (contains(key + LENGTH)) {
            // Workaround for pre-HC's lack of StringSets
            val stringSetLength = readInt(key + LENGTH)
            if (stringSetLength >= 0) {
                _sharedPreferences.edit().remove(key + LENGTH).apply()
                for (i in 0 until stringSetLength) {
                    _sharedPreferences.edit().remove("$key[$i]").apply()
                }
            }
        }
        _sharedPreferences.edit().remove(key).apply()
    }

    fun contains(key: String?): Boolean {
        return _sharedPreferences.contains(key)
    }

    fun clear() {
        _sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val LENGTH = "_length"
        private const val DEFAULT_STRING_VALUE = ""
        private const val DEFAULT_INT_VALUE = -1
        private const val DEFAULT_DOUBLE_VALUE = -1.0
        private const val DEFAULT_FLOAT_VALUE = -1f
        private const val DEFAULT_LONG_VALUE = -1L
        private const val DEFAULT_BOOLEAN_VALUE = false
        private var PrefsHomeInstance: Prefs? = null
        fun with(context: Context): Prefs? {
            if (PrefsHomeInstance == null) {
                PrefsHomeInstance = Prefs(context)
            }
            return PrefsHomeInstance
        }

        fun with(context: Context, forceInstantiation: Boolean): Prefs? {
            if (forceInstantiation) {
                PrefsHomeInstance = Prefs(context)
            }
            return PrefsHomeInstance
        }

        fun with(context: Context, preferencesName: String): Prefs? {
            if (PrefsHomeInstance == null) {
                PrefsHomeInstance = Prefs(context, preferencesName)
            }
            return PrefsHomeInstance
        }

        fun with(
            context: Context, preferencesName: String,
            forceInstantiation: Boolean
        ): Prefs? {
            if (forceInstantiation) {
                PrefsHomeInstance = Prefs(context, preferencesName)
            }
            return PrefsHomeInstance
        }
    }
}
