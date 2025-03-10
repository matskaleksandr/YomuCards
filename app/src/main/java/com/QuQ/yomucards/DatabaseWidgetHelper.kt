package com.QuQ.yomucards

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.File

class DatabaseWidgetHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "yomucardsdb.db"
        private const val DATABASE_VERSION = 1
    }

    fun isDatabaseExist(context: Context, dbName: String): Boolean {
        val dbFile = File(context.filesDir, dbName)
        return dbFile.exists()
    }

    fun getRandomCharacter(context: Context): String {
        val dbName = "yomucardsdb.db"
        val dbFile = File(context.filesDir, dbName)

        if (!isDatabaseExist(context, dbName)) {
            Log.e("Database", "Database does not exist!")
            return ""
        }

        val db: SQLiteDatabase = try {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: SQLiteException) {
            Log.e("Database", "Error opening database", e)
            return "" // Ошибка открытия базы данных
        }

        val queries = arrayOf(
            "SELECT kana, ENtranscription FROM Hiragana ORDER BY RANDOM() LIMIT 1",
            "SELECT kana, ENtranscription FROM Katakana ORDER BY RANDOM() LIMIT 1",
            "SELECT kanj, ENtranscription FROM Kanji ORDER BY RANDOM() LIMIT 1"
        )

        val randomQuery = queries.random()

        val cursor = db.rawQuery(randomQuery, null)

        var result = ""
        if (cursor.moveToFirst()) {
            val char = cursor.getString(0) // kana / kanj
            val transcription = cursor.getString(1) ?: "" // RUtranscription (может быть null)
            result = "$char - $transcription"
        }
        cursor.close()
        db.close()

        return result
    }

    override fun onCreate(db: SQLiteDatabase?) {}

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}
