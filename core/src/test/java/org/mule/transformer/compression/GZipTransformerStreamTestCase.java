/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.compression;

import org.mule.api.transformer.TransformerException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.lang.SerializationUtils;

public class GZipTransformerStreamTestCase extends GZipTransformerTestCase
{
    
    public void testStreamingCompression() throws TransformerException
    {
        GZipCompressTransformer transformer = new GZipCompressTransformer();
        
        InputStream input = new ByteArrayInputStream(SerializationUtils.serialize(TEST_DATA));

        byte[] expected = (byte[]) this.getResultData();
        byte[] result = (byte[]) transformer.transform(input);
        
        assertTrue(Arrays.equals(expected, result));
    }

    public void testStreamingDecompression() throws TransformerException
    {
        GZipUncompressTransformer transformer = new GZipUncompressTransformer();
        
        InputStream input = new ByteArrayInputStream((byte[]) this.getResultData());
        byte[] resultBytes = (byte[]) transformer.transform(input);
        assertEquals(TEST_DATA, SerializationUtils.deserialize(resultBytes));
    }

}


