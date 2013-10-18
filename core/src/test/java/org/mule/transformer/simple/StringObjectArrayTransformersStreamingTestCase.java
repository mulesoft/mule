/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class StringObjectArrayTransformersStreamingTestCase extends StringObjectArrayTransformersTestCase
{

    public Object getTestData()
    {
        String testData = (String) super.getTestData();
        return new ByteArrayInputStream(testData.getBytes());
    }

    public boolean compareRoundtripResults(Object src, Object result)
    {
        InputStream input = (InputStream) src;
        String expected = IOUtils.toString(input);
        
        return expected.equals(result);
    }
    
}


