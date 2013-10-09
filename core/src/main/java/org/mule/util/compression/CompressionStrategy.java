/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.compression;

import java.io.IOException;
import java.io.InputStream;

/**
 * <code>CompressionStrategy</code> is a base interface for Different compression
 * strategies
 */
public interface CompressionStrategy
{
    /**
     * The fully qualified class name of the fallback
     * <code>CompressionStrategy</code> implementation class to use, if no other
     * can be found. the default is
     * <code>org.mule.util.compression.GZipCompression</code>
     */
    String COMPRESSION_DEFAULT = "org.mule.util.compression.GZipCompression";

    /**
     * JDK1.3+ 'Service Provider' specification (
     * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html )
     */
    String SERVICE_ID = "META-INF/services/org.mule.util.compression.CompressionStrategy";

    /**
     * Compresses a byte array.
     *
     * @param bytes The byte array to compress.
     * @return The compressed byte array.
     * @throws IOException If an I/O error has occurred.
     */
    byte[] compressByteArray(byte[] bytes) throws IOException;

    /**
     * Compresses an {@link InputStream}.
     *
     * @param is The {@link InputStream} to compress.
     * @return The compressed {@link InputStream}.
     * @throws IOException If an I/O error has occurred.
     */
    InputStream compressInputStream(InputStream is) throws IOException;

    /**
     * Uncompresses a compressed byte array.
     *
     * @param bytes The byte array to uncompress.
     * @return The uncompressed byte array.
     * @throws IOException If an I/O error has occurred.
     */
    byte[] uncompressByteArray(byte[] bytes) throws IOException;

    /**
     * Uncompresses a compressed {@link InputStream}.
     *
     * @param is The compressed {@link InputStream}.
     * @return The uncompressed {@link InputStream}.
     * @throws IOException If an I/O error has occurred.
     */
    InputStream uncompressInputStream(InputStream is) throws IOException;

    /**
     * Checks whether a byte array has been compressed or not.
     *
     * @param bytes The byte array.
     * @return True if the byte array is compressed, false otherwise.
     * @throws IOException If an I/O error has occurred.
     */
    boolean isCompressed(byte[] bytes) throws IOException;
}
