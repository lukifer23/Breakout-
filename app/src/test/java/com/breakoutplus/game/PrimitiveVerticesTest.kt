package com.breakoutplus.game

import org.junit.Assert.*
import org.junit.Test

class PrimitiveVerticesTest {
    @Test fun rectangleTopologyColorAndOrderMatchTheFormerStrip() {
        val mesh = PrimitiveVertices(90)
        val c = floatArrayOf(.2f, .4f, .6f, .8f)
        mesh.rect(10f, 20f, 3f, 4f, c)
        mesh.prepare()
        assertEquals(6, mesh.count)
        val expected = listOf(10f to 20f, 13f to 20f, 10f to 24f, 10f to 24f, 13f to 20f, 13f to 24f)
        expected.forEachIndexed { i, point ->
            assertEquals(point.first, mesh.buffer.get(i * 6), 0f)
            assertEquals(point.second, mesh.buffer.get(i * 6 + 1), 0f)
            repeat(4) { assertEquals(c[it], mesh.buffer.get(i * 6 + 2 + it), 0f) }
        }
        // Source scratch color reuse cannot alter previously submitted vertices.
        c.fill(1f)
        assertEquals(.2f, mesh.buffer.get(2), 0f)
    }
    @Test fun circleRetains28SegmentFanGeometryAndBuffersReuse() {
        val mesh = PrimitiveVertices(90)
        mesh.rect(0f, 0f, 1f, 1f, floatArrayOf(1f, 1f, 1f, 1f))
        mesh.circle(5f, 6f, 2f, floatArrayOf(.5f, .5f, .5f, 1f))
        mesh.prepare()
        assertEquals(90, mesh.count)
        assertFalse(mesh.hasRoom(1))
        repeat(28) { i ->
            val base = (6 + i * 3) * 6
            assertEquals(5f, mesh.buffer.get(base), 0f)
            assertEquals(6f, mesh.buffer.get(base + 1), 0f)
            val dx = mesh.buffer.get(base + 6) - 5f
            val dy = mesh.buffer.get(base + 7) - 6f
            assertEquals(4f, dx * dx + dy * dy, .00001f)
        }
        val buffer = mesh.buffer
        mesh.clear(); assertSame(buffer, mesh.buffer); assertEquals(0, mesh.count)
    }
}
