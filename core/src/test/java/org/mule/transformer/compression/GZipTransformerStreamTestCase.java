/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.compression;

import org.mule.api.transformer.TransformerException;
import org.mule.util.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GZipTransformerStreamTestCase extends GZipTransformerTestCase
{

    @Test
    public void testStreamingCompression() throws TransformerException
    {
        GZipCompressTransformer transformer = new GZipCompressTransformer();
        
        InputStream input = new ByteArrayInputStream(SerializationUtils.serialize(TEST_DATA));

        byte[] expected = (byte[]) this.getResultData();
        byte[] result = (byte[]) transformer.transform(input);
        
        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testStreamingDecompression() throws TransformerException
    {
        GZipUncompressTransformer transformer = new GZipUncompressTransformer();
        transformer.setMuleContext(muleContext);
        
        InputStream input = new ByteArrayInputStream((byte[]) this.getResultData());
        String resultingString = (String) transformer.transform(input);
        assertEquals(TEST_DATA, resultingString);
    }

}


