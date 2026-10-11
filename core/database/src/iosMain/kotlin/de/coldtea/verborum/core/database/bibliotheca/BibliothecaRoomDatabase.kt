package de.coldtea.verborum.core.database.bibliotheca

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * The iOS database.
 *
 * The schema mirrors the Android app's, but the version numbering is this target's own and does not
 * track Android's: v1 was created at the shape Android had reached by *its* v3, because no iOS
 * device had ever held anything earlier. v2 adds the two tables for dictionaries downloaded from the
 * Forum — the same change Android made at its v4.
 */
@Database(
    entities = [
        RoomDictionary::class,
        RoomWord::class,
        RoomImportedDictionary::class,
        RoomImportedWord::class,
    ],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(BibliothecaRoomDatabaseConstructor::class)
internal abstract class BibliothecaRoomDatabase : RoomDatabase() {

    abstract fun dictionaryDao(): RoomDictionaryDao

    abstract fun wordDao(): RoomWordDao

    abstract fun importedDictionaryDao(): RoomImportedDictionaryDao

    abstract fun importedWordDao(): RoomImportedWordDao
}

/** Room's KSP processor writes the `actual` for each iOS target. */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
internal expect object BibliothecaRoomDatabaseConstructor :
    RoomDatabaseConstructor<BibliothecaRoomDatabase> {
    override fun initialize(): BibliothecaRoomDatabase
}
