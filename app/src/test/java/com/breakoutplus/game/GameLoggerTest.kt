package com.breakoutplus.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLoggerTest {

    @Test
    fun setEnabled_updatesInternalFlag() {
        val logger = allocateWithoutConstructor(GameLogger::class.java)
        val enabledField = GameLogger::class.java.getDeclaredField("enabled").apply {
            isAccessible = true
        }

        enabledField.setBoolean(logger, false)
        assertFalse(enabledField.getBoolean(logger))
        logger.setEnabled(true)
        assertTrue(enabledField.getBoolean(logger))
    }

    private fun <T> allocateWithoutConstructor(type: Class<T>): T {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null)
        @Suppress("UNCHECKED_CAST")
        return unsafeClass
            .getMethod("allocateInstance", Class::class.java)
            .invoke(unsafe, type) as T
    }
}
