package io.github.cluno1.sonorus.features.local.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RhythmDatabaseMigration10To11Test {
    @Test fun migrationCreatesAlbumTablesAndPreservesArtworkReference() {
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
            db.execSQL("CREATE TABLE songs (id TEXT PRIMARY KEY NOT NULL, albumId TEXT NOT NULL, album TEXT NOT NULL, artist TEXT NOT NULL, albumArtist TEXT)")
            db.execSQL("CREATE TABLE device_metadata (stableId TEXT PRIMARY KEY NOT NULL, songId TEXT NOT NULL, artworkProvider TEXT, artworkExternalId TEXT, artworkConfidence REAL, artworkSource TEXT, artworkCachePath TEXT, updatedAt INTEGER NOT NULL)")
            db.execSQL("INSERT INTO songs VALUES ('42', '7', 'Live Album', 'Artist', 'Album Artist')")
            db.execSQL("INSERT INTO device_metadata VALUES ('stable-42', '42', 'DEEZER', '123', 0.93, 'PUBLIC_API', '/cache/cover.jpg', 1000)")

            RhythmDatabase.MIGRATION_10_11.migrate(db)

            db.query("PRAGMA table_info(device_album_metadata)").use { cursor ->
                val names = mutableSetOf<String>()
                val index = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) names += cursor.getString(index)
                assertTrue(names.containsAll(setOf("albumKey", "externalReleaseGroupId", "artworkSha256", "negativeUntil")))
            }
            db.query("SELECT albumKey, provider, artworkCachePath FROM device_album_metadata").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("mediastore:external:7", cursor.getString(0))
                assertEquals("DEEZER", cursor.getString(1))
                assertEquals("/cache/cover.jpg", cursor.getString(2))
            }
            db.query("SELECT songStableId, albumKey FROM device_song_album").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("stable-42", cursor.getString(0))
                assertEquals("mediastore:external:7", cursor.getString(1))
            }
        }
        helper.close()
    }
}
