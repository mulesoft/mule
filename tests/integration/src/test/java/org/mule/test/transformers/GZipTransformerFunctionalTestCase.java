/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.IOUtils;
import org.mule.util.compression.GZipCompression;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

/**
 * Functional test case for the GZipCompressTransformer and GZipUncompressTransformer.
 */
public class GZipTransformerFunctionalTestCase extends FunctionalTestCase
{
    private static final String TEST_DATA = "the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/transformers/gzip-transformer-functional-test.xml";
    }

    @Test
    public void testCompressDecompressByteArray() throws Exception
    {
        byte[] testDataByteArray = TEST_DATA.getBytes();
        MuleClient client = muleContext.getClient();

        // Compress input.
        MuleMessage compressedResponse = client.send("vm://compressInput", testDataByteArray, null);
        assertNotNull(compressedResponse);
        assertTrue(compressedResponse.getPayload() instanceof byte[]);

        // Decompress response.
        MuleMessage uncompressedResponse = client.send("vm://decompressInput", compressedResponse.getPayload(), null);
        assertNotNull(uncompressedResponse);
        assertTrue(uncompressedResponse.getPayload() instanceof byte[]);

        String uncompressedStr = new String((byte[]) uncompressedResponse.getPayload());
        assertEquals(TEST_DATA, uncompressedStr);
    }

    @Test
    public void testCompressDecompressInputStream() throws Exception
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(TEST_DATA.getBytes());
        MuleClient client = muleContext.getClient();

        // Compress input.
        MuleMessage compressedResponse = client.send("vm://compressInput", bis, null);
        assertNotNull(compressedResponse);
        assertTrue(compressedResponse.getPayload() instanceof InputStream);

        // Decompress response.
        MuleMessage uncompressedResponse = client.send("vm://decompressInput", compressedResponse.getPayload(), null);
        assertNotNull(uncompressedResponse);
        assertTrue(uncompressedResponse.getPayload() instanceof InputStream);

        byte[] uncompressedByteArray = IOUtils.toByteArray((InputStream) uncompressedResponse.getPayload());
        String uncompressedStr = new String(uncompressedByteArray);
        assertEquals(TEST_DATA, uncompressedStr);
    }

    @Test
    public void testCompressDecompressString() throws Exception
    {
        MuleClient client = muleContext.getClient();

        // Compress input.
        MuleMessage compressedResponse = client.send("vm://compressInput", TEST_DATA, null);
        assertNotNull(compressedResponse);
        assertTrue(compressedResponse.getPayload() instanceof byte[]);
        byte[] bytes = new GZipCompression().uncompressByteArray((byte[]) compressedResponse.getPayload());
        String clientUncompressed = new String(bytes, "UTF8");
        assertEquals(TEST_DATA, clientUncompressed);

        // Decompress response.
        MuleMessage uncompressedResponse = client.send("vm://decompressInputString", compressedResponse.getPayload(), null);
        assertNotNull(uncompressedResponse);
        assertTrue(uncompressedResponse.getPayload() instanceof String);
        assertEquals(TEST_DATA, uncompressedResponse.getPayload());

    }

}
