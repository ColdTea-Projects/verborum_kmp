package de.coldtea.verborum.core.database.bibliotheca

/**
 * The library's local database: the same two tables the Android app keeps in Room, behind an
 * interface `commonMain` can name.
 *
 * Only iOS has one. `createBibliothecaDatabase` answers null on web, where the app keeps its state in
 * memory for the session — persisting a user's own content in browser storage is a decision about
 * data at rest, not a detail to slip in with a database.
 */
interface BibliothecaDatabase {

    val dictionaryDao: DictionaryDao

    val wordDao: WordDao

    /**
     * Runs [block] as one unit of work, so a half-applied merge is never visible.
     *
     * Reads and writes inside must go through this database's DAOs; a nested call is not supported.
     */
    suspend fun <R> withTransaction(block: suspend () -> R): R

    /** Drops every row. Sign-out clears the local library along with the tokens. */
    suspend fun clear()
}

/** The database for this target, or null where the app keeps no local copy. */
expect fun createBibliothecaDatabase(): BibliothecaDatabase?
