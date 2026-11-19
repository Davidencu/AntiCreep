package com.example.detectorulcreeps

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Baza de date SQLite cu rapoartele salvate (CRUD: create, read, update, delete).
 * Operațiile rulează pe Dispatchers.IO, ca interfața să nu se blocheze.
 */
class ReportDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $NAME TEXT NOT NULL,
                $NOTE TEXT NOT NULL,
                $CREATED_AT INTEGER NOT NULL,
                $LEVEL TEXT NOT NULL,
                $COMBINED REAL NOT NULL,
                $MAGNETIC REAL NOT NULL,
                $OPTIC REAL NOT NULL,
                $MAGNETIC_FLAG INTEGER NOT NULL,
                $OPTIC_FLAG INTEGER NOT NULL,
                $SNAPSHOT BLOB,
                $NETWORK_FLAG INTEGER NOT NULL DEFAULT 0,
                $NETWORK_COMPLETE INTEGER NOT NULL DEFAULT 0,
                $NETWORK_DEVICES TEXT NOT NULL DEFAULT '',
                $CHECKED_ZONES TEXT NOT NULL DEFAULT '',
                $ANGLE_CHECK TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versiunea 2 adaugă imaginea zonei suspecte; rapoartele vechi rămân, fără imagine.
        if (oldVersion < 2) db.execSQL("ALTER TABLE $TABLE ADD COLUMN $SNAPSHOT BLOB")
        // Versiunea 3 adaugă triajul de rețea. Rapoartele vechi nu au rulat triajul, deci apar ca incomplete.
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $NETWORK_FLAG INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $NETWORK_COMPLETE INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $NETWORK_DEVICES TEXT NOT NULL DEFAULT ''")
        }
        // Versiunea 4 adaugă zonele bifate și verificarea unghiului.
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $CHECKED_ZONES TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $ANGLE_CHECK TEXT")
        }
    }

    /** Create: salvează raportul și întoarce id-ul lui. */
    suspend fun create(report: Report): Long = withContext(Dispatchers.IO) {
        writableDatabase.insertOrThrow(TABLE, null, report.toValues())
    }

    /** Read: toate rapoartele, cele mai noi primele. */
    suspend fun getAll(): List<Report> = withContext(Dispatchers.IO) {
        readableDatabase.query(TABLE, null, null, null, null, null, "$CREATED_AT DESC, $ID DESC").use { c ->
            buildList { while (c.moveToNext()) add(c.toReport()) }
        }
    }

    /** Read: un singur raport, sau null dacă nu există. */
    suspend fun get(id: Long): Report? = withContext(Dispatchers.IO) {
        readableDatabase.query(TABLE, null, "$ID = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) c.toReport() else null
        }
    }

    /** Update: schimbă numele și nota raportului. */
    suspend fun update(id: Long, name: String, note: String): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(NAME, name.trim())
            put(NOTE, note.trim())
        }
        writableDatabase.update(TABLE, values, "$ID = ?", arrayOf(id.toString())) > 0
    }

    /** Delete: șterge raportul. */
    suspend fun delete(id: Long): Boolean = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE, "$ID = ?", arrayOf(id.toString())) > 0
    }

    /** Șterge toate rapoartele (din ecranul Setări). */
    suspend fun deleteAll(): Int = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE, null, null)
    }

    private fun Report.toValues() = ContentValues().apply {
        put(NAME, name.trim())
        put(NOTE, note.trim())
        put(CREATED_AT, createdAt)
        put(LEVEL, result.level.name)
        put(COMBINED, result.combinedScore)
        put(MAGNETIC, result.magneticScore)
        put(OPTIC, result.opticScore)
        put(MAGNETIC_FLAG, if (result.magneticFlag) 1 else 0)
        put(OPTIC_FLAG, if (result.opticFlag) 1 else 0)
        put(SNAPSHOT, snapshot)
        put(NETWORK_FLAG, if (result.networkFlag) 1 else 0)
        put(NETWORK_COMPLETE, if (result.networkComplete) 1 else 0)
        put(NETWORK_DEVICES, result.networkDevices)
        put(CHECKED_ZONES, result.checkedZones)
        put(ANGLE_CHECK, result.angleCheck?.name)
    }

    private fun Cursor.toReport() = Report(
        id = getLong(getColumnIndexOrThrow(ID)),
        name = getString(getColumnIndexOrThrow(NAME)),
        note = getString(getColumnIndexOrThrow(NOTE)),
        createdAt = getLong(getColumnIndexOrThrow(CREATED_AT)),
        result = FusionResult(
            level = RiskLevel.valueOf(getString(getColumnIndexOrThrow(LEVEL))),
            combinedScore = getFloat(getColumnIndexOrThrow(COMBINED)),
            magneticScore = getFloat(getColumnIndexOrThrow(MAGNETIC)),
            opticScore = getFloat(getColumnIndexOrThrow(OPTIC)),
            magneticFlag = getInt(getColumnIndexOrThrow(MAGNETIC_FLAG)) == 1,
            opticFlag = getInt(getColumnIndexOrThrow(OPTIC_FLAG)) == 1,
            networkFlag = getInt(getColumnIndexOrThrow(NETWORK_FLAG)) == 1,
            networkComplete = getInt(getColumnIndexOrThrow(NETWORK_COMPLETE)) == 1,
            networkDevices = getString(getColumnIndexOrThrow(NETWORK_DEVICES)),
            checkedZones = getString(getColumnIndexOrThrow(CHECKED_ZONES)),
            angleCheck = getColumnIndexOrThrow(ANGLE_CHECK).let { i ->
                if (isNull(i)) null else runCatching { AngleVerdict.valueOf(getString(i)) }.getOrNull()
            },
        ),
        snapshot = getColumnIndexOrThrow(SNAPSHOT).let { if (isNull(it)) null else getBlob(it) },
    )

    private companion object {
        const val DB_NAME = "rapoarte.db"
        const val DB_VERSION = 4
        const val TABLE = "reports"
        const val ID = "id"
        const val NAME = "name"
        const val NOTE = "note"
        const val CREATED_AT = "created_at"
        const val LEVEL = "level"
        const val COMBINED = "combined_score"
        const val MAGNETIC = "magnetic_score"
        const val OPTIC = "optic_score"
        const val MAGNETIC_FLAG = "magnetic_flag"
        const val OPTIC_FLAG = "optic_flag"
        const val SNAPSHOT = "snapshot"
        const val NETWORK_FLAG = "network_flag"
        const val NETWORK_COMPLETE = "network_complete"
        const val NETWORK_DEVICES = "network_devices"
        const val CHECKED_ZONES = "checked_zones"
        const val ANGLE_CHECK = "angle_check"
    }
}
