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
class RhythmDatabaseMigration11To12Test {
    @Test fun migrationAddsPinnedPublicDetailOverridesWithoutChangingExistingRows() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                }).build()
        )
        helper.writableDatabase.use { db ->
            db.execSQL(
                """
                CREATE TABLE device_metadata (
                    stableId TEXT PRIMARY KEY NOT NULL,
                    songId TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO device_metadata VALUES ('stable-42', '42', 1000)")

            RhythmDatabase.MIGRATION_11_12.migrate(db)

            db.query("PRAGMA table_info(device_metadata)").use { cursor ->
                val names = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) names += cursor.getString(nameIndex)
                assertTrue(
                    names.containsAll(
                        setOf(
                            "detailsProvider",
                            "detailsExternalId",
                            "detailsConfidence",
                            "detailsPinned",
                            "titleOverride",
                            "artistOverride",
                            "albumOverride",
                            "albumArtistOverride",
                            "yearOverride",
                            "trackNumberOverride",
                            "discNumberOverride",
                            "genreOverride",
                        )
                    )
                )
            }
            db.query("SELECT songId, detailsPinned, titleOverride FROM device_metadata").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("42", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
                assertTrue(cursor.isNull(2))
            }
        }
        helper.close()
    }
}
