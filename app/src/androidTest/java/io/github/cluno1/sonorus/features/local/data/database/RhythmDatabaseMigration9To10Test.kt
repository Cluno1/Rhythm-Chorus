package io.github.cluno1.sonorus.features.local.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RhythmDatabaseMigration9To10Test {
    @Test fun migrationCreatesDeviceMetadataAndPinnedColumn() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        helper.writableDatabase.use { db ->
            RhythmDatabase.MIGRATION_9_10.migrate(db)
            db.query("PRAGMA table_info(device_metadata)").use { cursor ->
                val names = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) names += cursor.getString(nameIndex)
                assertTrue(names.containsAll(setOf("stableId", "fingerprint", "lyricsProvider", "lyricsPinned", "artworkCachePath")))
            }
        }
        helper.close()
    }
}
