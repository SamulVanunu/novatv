package dev.anon.novatv.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v1 → v2 migration'ı: dört yeni nullable kolon eklenir, mevcut kayıtlar korunur.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NovaDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2AddsNullableHeaderAndErrorColumns() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO playlist
                    (id, type, name, baseUrl, username, epgUrl, userAgent,
                     isActive, sortOrder, lastSyncAt, syncState, syncIntervalHours)
                VALUES (1, 'M3U', 'Kaynak', 'http://example.test/list.m3u', NULL, NULL,
                        NULL, 1, 0, NULL, 'OK', 24)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO channel
                    (playlistId, remoteId, number, name, nameNormalized, groupName,
                     logoUrl, tvgId, hasArchive, archiveDays, streamFormat, syncToken)
                VALUES (1, 'r1', NULL, 'TRT 1', 'trt 1', 'Ulusal',
                        NULL, NULL, 0, 0, 'AUTO', 1)
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT referer, lastError FROM playlist").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
            assertTrue(cursor.isNull(1))
        }
        db.query("SELECT streamUserAgent, streamReferer FROM channel").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
            assertTrue(cursor.isNull(1))
        }
    }

    private companion object {
        const val TEST_DB = "novatv-migration-test.db"
    }
}
