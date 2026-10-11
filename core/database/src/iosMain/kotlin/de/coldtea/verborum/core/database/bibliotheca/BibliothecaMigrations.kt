package de.coldtea.verborum.core.database.bibliotheca

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds the two tables for dictionaries downloaded from the Forum.
 *
 * The statements are **Room's own DDL**, copied verbatim from the exported `schemas/…/2.json` with
 * `${'$'}{TABLE_NAME}` resolved. That is not pedantry: Room validates the tables it opens against the
 * shape it computed at compile time, and a migration whose `CREATE TABLE` differs in column order,
 * nullability or index name fails the check and takes the whole database down on launch — so the
 * only safe source for this SQL is the schema Room exported, not a hand-written equivalent.
 *
 * Nothing is copied or dropped: v1 held no downloaded dictionaries, so there is nothing to carry
 * over, and the user's own two tables are untouched.
 */
internal val MIGRATION_1_2 = object : Migration(startVersion = 1, endVersion = 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `imported_dictionary` (" +
                "`dictionary_id` TEXT NOT NULL, " +
                "`publisher_id` TEXT NOT NULL, " +
                "`publisher_name` TEXT, " +
                "`name` TEXT NOT NULL, " +
                "`from_lang` TEXT NOT NULL, " +
                "`to_lang` TEXT NOT NULL, " +
                "`tags` TEXT NOT NULL, " +
                "`imported_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`dictionary_id`))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `imported_word` (" +
                "`word_id` TEXT NOT NULL, " +
                "`fk_dictionary_id` TEXT NOT NULL, " +
                "`word` TEXT NOT NULL, " +
                "`word_meta` TEXT NOT NULL, " +
                "`translation` TEXT NOT NULL, " +
                "`translation_meta` TEXT NOT NULL, " +
                "`level` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`word_id`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_imported_word_fk_dictionary_id` " +
                "ON `imported_word` (`fk_dictionary_id`)",
        )
    }
}
