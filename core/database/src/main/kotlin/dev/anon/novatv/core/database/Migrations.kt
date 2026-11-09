package dev.anon.novatv.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: akış başlıkları ve senkron hatası.
 *
 * - `playlist.referer`, `playlist.lastError`
 * - `channel.streamUserAgent`, `channel.streamReferer`
 *
 * Hepsi nullable; mevcut kayıtlar başlıksız olarak çalışmaya devam eder.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE playlist ADD COLUMN referer TEXT")
        db.execSQL("ALTER TABLE playlist ADD COLUMN lastError TEXT")
        db.execSQL("ALTER TABLE channel ADD COLUMN streamUserAgent TEXT")
        db.execSQL("ALTER TABLE channel ADD COLUMN streamReferer TEXT")
    }
}
