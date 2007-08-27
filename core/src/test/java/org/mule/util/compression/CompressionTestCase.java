/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.compression;

import org.mule.tck.AbstractMuleTestCase;

import java.util.Arrays;

public class CompressionTestCase extends AbstractMuleTestCase
{

    public void testCompressDefaultGZip() throws Exception
    {
        String temp = "This is a compressed string";
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        byte[] compressed = strategy.compressByteArray(temp.getBytes());

        // For small test data the compressed data will be bigger than the real data
        assertTrue(compressed.length > temp.getBytes().length);

        byte[] uncompressed = strategy.uncompressByteArray(compressed);
        assertTrue(uncompressed.length == temp.getBytes().length);

        assertEquals(temp, new String(uncompressed));

        String tempLarge = temp;
        for (int i = 0; i < 100; i++)
        {
            tempLarge += temp;
        }

        compressed = strategy.compressByteArray(tempLarge.getBytes());

        assertTrue(compressed.length < tempLarge.getBytes().length);

        uncompressed = strategy.uncompressByteArray(compressed);
        assertTrue(uncompressed.length == tempLarge.getBytes().length);

        assertEquals(tempLarge, new String(uncompressed));

    }

    public void testNullIsCompressed() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        assertFalse(strategy.isCompressed(null));
    }

    public void testEmptyIsCompressed() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        assertFalse(strategy.isCompressed(new byte[0]));
    }

    public void testCompressNullBytes() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        assertNull(strategy.compressByteArray(null));
    }

    public void testCompressEmptyBytes() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        byte[] bytes = new byte[0];
        byte[] result = strategy.compressByteArray(bytes);

        assertTrue(strategy.isCompressed(result));
    }

    public void testUncompressNullBytes() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        assertNull(strategy.uncompressByteArray(null));
    }

    public void testUncompressEmptyBytes() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        byte[] bytes = new byte[0];

        byte[] cmpbytes = strategy.compressByteArray(bytes);
        assertTrue(strategy.isCompressed(cmpbytes));

        byte[] result = strategy.uncompressByteArray(cmpbytes);
        assertTrue(Arrays.equals(bytes, result));
    }

}
