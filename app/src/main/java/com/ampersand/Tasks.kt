package com.ampersand

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspends until a Play Services [Task] finishes: its result, or its own exception
 * rethrown unchanged (not wrapped in a cancellation), so callers can match on it.
 * The one bridge from Firebase's callbacks to coroutines.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
