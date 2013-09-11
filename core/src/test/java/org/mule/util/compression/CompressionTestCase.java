/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.compression;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SmallTest
public class CompressionTestCase extends AbstractMuleTestCase
{

    @Test
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

    @Test
    public void testNullIsCompressed() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        assertFalse(strategy.isCompressed(null));
    }

    @Test
    public void testEmptyIsCompressed() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        assertFalse(strategy.isCompressed(new byte[0]));
    }

    @Test
    public void testCompressNullBytes() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        assertNull(strategy.compressByteArray(null));
    }

    @Test
    public void testCompressEmptyBytes() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        byte[] bytes = new byte[0];
        byte[] result = strategy.compressByteArray(bytes);

        assertTrue(strategy.isCompressed(result));
    }

    @Test
    public void testUncompressNullBytes() throws Exception
    {
        CompressionStrategy strategy = CompressionHelper.getDefaultCompressionStrategy();
        assertNull(strategy.uncompressByteArray(null));
    }

    @Test
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
