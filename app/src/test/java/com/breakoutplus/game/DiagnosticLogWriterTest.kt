package com.breakoutplus.game

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class DiagnosticLogWriterTest {
    @Test fun actualBackgroundWritesPreserveOrderAndReportFailures() {
        val directory = Files.createTempDirectory("breakout-logs").toFile()
        try {
            val writer = DiagnosticLogWriter<Int>(directory, "test", encode = { it.toString() })
            writer.append((0 until 100).toList())
            writer.append((100 until 200).toList())
            assertTrue(writer.finish())
            assertNull(writer.lastFailure)
            assertEquals((0 until 200).map { it.toString() }, directory.listFiles()!!.single().readLines())
            assertEquals(0L, writer.droppedBatches.get())
        } finally { directory.deleteRecursively() }
    }

    @Test fun realLogFilesRotateAndPruneToTheConfiguredBound() {
        val directory = Files.createTempDirectory("breakout-logs").toFile()
        try {
            val writer = DiagnosticLogWriter<Int>(directory, "test", encode = { "$it-event" }, maxFileBytes = 40, maxFiles = 3)
            writer.append((0 until 100).toList())
            assertTrue(writer.finish())
            assertNull(writer.lastFailure)
            assertEquals(3, directory.listFiles()!!.size)
            assertTrue(directory.listFiles()!!.all { it.length() <= 40 })
        } finally { directory.deleteRecursively() }
    }
}
