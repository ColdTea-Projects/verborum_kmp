package de.coldtea.verborum.core.database.bibliotheca

/** No local database in the browser — the app's stores stay in memory for the session. */
actual fun createBibliothecaDatabase(): BibliothecaDatabase? = null
