package com.breakoutplus.game

import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Bounded, ordered local diagnostics. Queue overflow drops diagnostics, never gameplay. */
class DiagnosticLogWriter<T>(
    private val directory: File,
    private val session: String,
    private val encode: (T) -> String,
    private val maxFileBytes: Long = 1024 * 1024,
    private val maxFiles: Int = 8
) {
    val droppedBatches = AtomicLong()
    @Volatile var lastFailure: Exception? = null
        private set
    private var part = 0
    private val executor = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(8),
        { task -> Thread(task, "BreakoutDiagnostics").apply { isDaemon = true } },
        { _, _ -> droppedBatches.incrementAndGet() })

    fun append(events: List<T>) {
        if (events.isEmpty()) return
        val immutable = events.toList()
        executor.execute {
            try {
                check(directory.isDirectory || directory.mkdirs())
                for (event in immutable) {
                    val line = encode(event) + "\n"
                    var file = File(directory, "session_${session}_${part}.jsonl")
                    if (file.length() + line.toByteArray(Charsets.UTF_8).size > maxFileBytes) {
                        part++; file = File(directory, "session_${session}_${part}.jsonl")
                    }
                    // A single oversized diagnostic must not bypass the disk bound.
                    if (line.toByteArray(Charsets.UTF_8).size <= maxFileBytes) file.appendText(line)
                }
                directory.listFiles()?.filter { it.isFile && it.name.startsWith("session_") }
                    ?.sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })
                    ?.drop(maxFiles)?.forEach { check(it.delete()) }
            } catch (e: Exception) { lastFailure = e }
        }
    }

    fun close() { executor.shutdown() }

    fun awaitIdle(timeoutSeconds: Long = 3): Boolean {
        val latch = java.util.concurrent.CountDownLatch(1)
        executor.execute { latch.countDown() }
        return latch.await(timeoutSeconds, TimeUnit.SECONDS)
    }

    /** Explicit export/shutdown only; never called by render/update. */
    fun finish(timeoutSeconds: Long = 3): Boolean {
        close()
        return executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)
    }
}
