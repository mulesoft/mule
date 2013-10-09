/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.IOUtils;
import org.mule.util.SerializationUtils;

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
    protected String getConfigResources()
    {
        return "org/mule/test/transformers/gzip-transformer-functional-test.xml";
    }

    @Test
    public void testCompressDecompressByteArray() throws Exception
    {
        byte[] testDataByteArray = TEST_DATA.getBytes();
        MuleClient client = new MuleClient(muleContext);

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
        MuleClient client = new MuleClient(muleContext);

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
}
