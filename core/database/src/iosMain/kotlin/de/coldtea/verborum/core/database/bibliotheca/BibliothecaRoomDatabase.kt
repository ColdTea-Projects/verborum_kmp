package de.coldtea.verborum.core.database.bibliotheca

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * The iOS database.
 *
 * The schema is the Android app's at *its* version 3 — tombstones and dictionary tags included — but
 * the version here starts at 1 on purpose: no iOS device has ever held a v1 or v2 file, so replaying
 * Android's migration history would be ceremony over a database that is created at the final shape.
 * A future schema change adds a migration from this 1, independently of Android's numbering.
 */
@Database(
    entities = [RoomDictionary::class, RoomWord::class],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(BibliothecaRoomDatabaseConstructor::class)
internal abstract class BibliothecaRoomDatabase : RoomDatabase() {

    abstract fun dictionaryDao(): RoomDictionaryDao

    abstract fun wordDao(): RoomWordDao
}

/** Room's KSP processor writes the `actual` for each iOS target. */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
internal expect object BibliothecaRoomDatabaseConstructor :
    RoomDatabaseConstructor<BibliothecaRoomDatabase> {
    override fun initialize(): BibliothecaRoomDatabase
}
