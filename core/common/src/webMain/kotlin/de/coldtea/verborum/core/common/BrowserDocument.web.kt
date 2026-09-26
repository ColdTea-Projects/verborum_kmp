package de.coldtea.verborum.core.common

/**
 * `document.title` — the browser tab's name, and the name a bookmark of this page is saved under.
 * Reached through per-target `js(...)` bridges because `js(...)` bodies are not allowed in a shared
 * intermediate source set.
 *
 * The bridge is data-only: a title is written as text, never as markup.
 */
expect fun setDocumentTitle(title: String)
