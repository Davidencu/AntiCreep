package com.example.detectorulcreeps

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Rulează pe telefon sau emulator: SQLite real, fără mock-uri. */
@RunWith(AndroidJUnit4::class)
class ReportDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fusion = FusionModule()
    private lateinit var db: ReportDatabase

    @Before
    fun setUp() {
        context.deleteDatabase(DB_NAME)
        db = ReportDatabase(context)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(DB_NAME)
    }

    private fun report(
        name: String = "Hotel Cluj",
        createdAt: Long = 1_000L,
        result: FusionResult = fusion.fuse(0.2f, 0.1f, NetworkTriageResult(emptyList())),
        snapshot: ByteArray? = null,
    ) = Report(name = name, note = "notă", createdAt = createdAt, result = result, snapshot = snapshot)

    @Test
    fun create_apoi_get_pastreaza_toate_campurile() = runBlocking {
        val triage = NetworkTriageResult(listOf(NetworkDevice("192.168.1.9", listOf(554))))
        val saved = report(result = fusion.fuse(0.9f, 0.1f, triage), snapshot = byteArrayOf(1, 2, 3))
        val id = db.create(saved)

        val loaded = db.get(id)
        assertNotNull(loaded)
        loaded!!
        assertEquals("Hotel Cluj", loaded.name)
        assertEquals("notă", loaded.note)
        assertEquals(RiskLevel.HIGH, loaded.result.level)
        assertTrue(loaded.result.magneticFlag)
        assertFalse(loaded.result.opticFlag)
        assertTrue(loaded.result.networkFlag)
        assertTrue(loaded.result.networkComplete)
        assertEquals("192.168.1.9 · porturi 554", loaded.result.networkDevices)
        assertArrayEquals(byteArrayOf(1, 2, 3), loaded.snapshot)
    }

    @Test
    fun un_triaj_incomplet_se_pastreaza_ca_incomplet() = runBlocking {
        val id = db.create(report(result = fusion.fuse(0f, 0f, NetworkTriageResult(emptyList(), "refuzat"))))
        val loaded = db.get(id)!!
        assertFalse(loaded.result.networkComplete)
        assertFalse(loaded.result.networkFlag)
        assertEquals("", loaded.result.networkDevices)
        assertEquals(RiskLevel.LOW, loaded.result.level)
    }

    @Test
    fun raportul_fara_imagine_are_snapshot_null() = runBlocking {
        val id = db.create(report(snapshot = null))
        assertNull(db.get(id)!!.snapshot)
    }

    @Test
    fun getAll_intoarce_cele_mai_noi_primele() = runBlocking {
        db.create(report(name = "vechi", createdAt = 1_000L))
        db.create(report(name = "nou", createdAt = 3_000L))
        db.create(report(name = "mijloc", createdAt = 2_000L))
        assertEquals(listOf("nou", "mijloc", "vechi"), db.getAll().map { it.name })
    }

    @Test
    fun update_schimba_doar_numele_si_nota() = runBlocking {
        val id = db.create(report())
        assertTrue(db.update(id, "  Apartament  ", " altă notă "))
        val loaded = db.get(id)!!
        assertEquals("Apartament", loaded.name)
        assertEquals("altă notă", loaded.note)
        assertEquals(1_000L, loaded.createdAt)
    }

    @Test
    fun update_si_delete_pe_un_id_inexistent_intorc_false() = runBlocking {
        assertFalse(db.update(999, "x", "y"))
        assertFalse(db.delete(999))
    }

    @Test
    fun delete_sterge_raportul() = runBlocking {
        val keep = db.create(report(name = "pastrat"))
        val gone = db.create(report(name = "sters"))
        assertTrue(db.delete(gone))
        assertNull(db.get(gone))
        assertEquals(listOf(keep), db.getAll().map { it.id })
    }

    @Test
    fun migrarea_din_versiunea_2_pastreaza_rapoartele_si_le_marcheaza_ca_fara_triaj() = runBlocking {
        db.close()
        context.deleteDatabase(DB_NAME)
        // Baza de date așa cum o crea versiunea 2 a aplicației, cu un raport existent.
        context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL(
                """
                CREATE TABLE reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL, note TEXT NOT NULL, created_at INTEGER NOT NULL,
                    level TEXT NOT NULL, combined_score REAL NOT NULL, magnetic_score REAL NOT NULL,
                    optic_score REAL NOT NULL, magnetic_flag INTEGER NOT NULL, optic_flag INTEGER NOT NULL,
                    snapshot BLOB
                )
                """.trimIndent()
            )
            old.execSQL(
                "INSERT INTO reports (name, note, created_at, level, combined_score, magnetic_score, " +
                    "optic_score, magnetic_flag, optic_flag) VALUES ('vechi', 'n', 5, 'MEDIUM', 0.5, 0.9, 0.1, 1, 0)"
            )
            old.version = 2
        }

        db = ReportDatabase(context)
        val migrated = db.getAll().single()
        assertEquals("vechi", migrated.name)
        assertEquals(RiskLevel.MEDIUM, migrated.result.level)
        assertTrue(migrated.result.magneticFlag)
        assertFalse(migrated.result.networkFlag)
        assertFalse(migrated.result.networkComplete)
        assertEquals("", migrated.result.networkDevices)
        assertEquals("", migrated.result.checkedZones)
        assertEquals(null, migrated.result.angleCheck)

        // Baza migrată acceptă rapoarte noi.
        val id = db.create(report(result = fusion.fuse(0f, 0f, NetworkTriageResult(emptyList()))))
        assertTrue(db.get(id)!!.result.networkComplete)
    }

    private companion object {
        const val DB_NAME = "rapoarte.db"
    }
}
