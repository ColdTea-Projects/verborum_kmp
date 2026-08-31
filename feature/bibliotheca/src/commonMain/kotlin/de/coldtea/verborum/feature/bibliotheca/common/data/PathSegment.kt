package de.coldtea.verborum.feature.bibliotheca.common.data

import io.ktor.http.encodeURLPathPart

/**
 * Pins an id to exactly one path segment.
 *
 * These endpoints carry ids in the path, and the ids are not this client's to trust: they come back
 * in server payloads, where `WordDto.wordId` is a nullable string with no shape enforced anywhere.
 * Interpolated raw, an id stops being data and becomes routing — a `/` reaches a different
 * endpoint, a `?` turns the remainder into a query string, a `#` truncates the path.
 *
 * A UUID passes through untouched, so this costs nothing in the case that is always expected.
 */
internal fun String.asPathSegment(): String = encodeURLPathPart()
