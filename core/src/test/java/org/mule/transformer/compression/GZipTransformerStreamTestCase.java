/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.compression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.transformer.TransformerException;
import org.mule.util.IOUtils;
import org.mule.util.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

public class GZipTransformerStreamTestCase extends GZipTransformerTestCase
{
    
    @Test
    public void testStreamingCompression() throws TransformerException
    {
        GZipCompressTransformer transformer = (GZipCompressTransformer) super.getTransformer();
        
        InputStream uncompressedInputStream = new ByteArrayInputStream(SerializationUtils.serialize(TEST_DATA));

        // Compress input data.
        InputStream compressedInputStream = (InputStream) transformer.transform(uncompressedInputStream);

        byte[] compressedBytes = (byte[]) getResultData();
        assertTrue(Arrays.equals(compressedBytes, IOUtils.toByteArray(compressedInputStream)));

        IOUtils.closeQuietly(uncompressedInputStream);
        IOUtils.closeQuietly(compressedInputStream);
    }

    @Test
    public void testStreamingDecompression() throws TransformerException
    {
        GZipUncompressTransformer transformer = new GZipUncompressTransformer();
        transformer.setMuleContext(muleContext);

        InputStream compressedInputStream = new ByteArrayInputStream((byte[]) getResultData());

        // Decompress the input data.
        InputStream decompressedInputStream = (InputStream) transformer.transform(compressedInputStream);

        assertEquals(TEST_DATA, SerializationUtils.deserialize(IOUtils.toByteArray(decompressedInputStream)));

        IOUtils.closeQuietly(compressedInputStream);
        IOUtils.closeQuietly(decompressedInputStream);
    }

    @Test
    public void testStreamingCompressionDecompression() throws TransformerException
    {
        GZipCompressTransformer compressTransformer = (GZipCompressTransformer) super.getTransformer();
        GZipUncompressTransformer decompressTransformer = new GZipUncompressTransformer();
        decompressTransformer.setMuleContext(muleContext);

        InputStream input = new ByteArrayInputStream(SerializationUtils.serialize(TEST_DATA));

        // Compress the input data.
        InputStream compressedInputStream = (InputStream) compressTransformer.transform(input);
        // Decompress the input data.
        InputStream decompressedInputStream = (InputStream) decompressTransformer.transform(compressedInputStream);

        assertEquals(SerializationUtils.deserialize(IOUtils.toByteArray(decompressedInputStream)), TEST_DATA);

        IOUtils.closeQuietly(compressedInputStream);
        IOUtils.closeQuietly(decompressedInputStream);
    }
}


