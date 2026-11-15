package com.abhi.swiftdm.engine

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Positional writer shared by all segment workers. [FileChannel.write(ByteBuffer, long)] is safe
 * for concurrent use, so no lock is needed around the actual disk write.
 */
internal class SegmentFileWriter(
    file: File,
    expectedTotalBytes: Long,
) : AutoCloseable {

    private val randomAccessFile = RandomAccessFile(file, "rw")
    private val channel: FileChannel = randomAccessFile.channel
    @Volatile
    private var closed = false

    init {
        if (expectedTotalBytes > 0 && randomAccessFile.length() != expectedTotalBytes) {
            randomAccessFile.setLength(expectedTotalBytes)
        }
    }

    fun write(bytes: ByteArray, offset: Int, length: Int, position: Long) {
        if (length <= 0) return
        val buffer = ByteBuffer.wrap(bytes, offset, length)
        var cursor = position
        while (buffer.hasRemaining()) {
            cursor += channel.write(buffer, cursor)
        }
    }

    fun truncate() {
        if (closed) return
        channel.truncate(0)
    }

    fun flush() {
        if (!closed) channel.force(false)
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { channel.force(false) }
        runCatching { channel.close() }
        runCatching { randomAccessFile.close() }
    }
}
