package com.alick.utilslibrary


class StorageUtils {
    companion object {

        fun getString(key: String): String {
            val sharedPreferences = getSharedPreferences()
            return sharedPreferences.getString(key, "") ?: ""
        }

        fun getInt(key: String): Int {
            val sharedPreferences = getSharedPreferences()
            return sharedPreferences.getInt(key, 0) ?: 0
        }

        fun setString(key: String, value: String) {
            val sharedPreferences = getSharedPreferences()
            sharedPreferences.edit().apply {
                this.putString(key, value)
                this.apply()
            }
        }

        fun setInt(key: String, value: Int) {
            val sharedPreferences = getSharedPreferences()
            sharedPreferences.edit().apply {
                this.putInt(key, value)
                this.apply()
            }
        }

        private fun getSharedPreferences() = AppHolder.getApp().getSharedPreferences("config", 0)
    }
}