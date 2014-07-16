/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;

import java.util.Arrays;

public class StringByteArrayTransformersTestCase extends AbstractTransformerTestCase
{

    public Transformer getTransformer() throws Exception
    {
        return new ObjectToByteArray();
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        return new ByteArrayToObject();
    }

    public Object getTestData()
    {
        return "Test";
    }

    public Object getResultData()
    {
        return "Test".getBytes();
    }

    @Override
    public boolean compareResults(Object src, Object result)
    {
        if (src == null && result == null)
        {
            return true;
        }
        if (src == null || result == null)
        {
            return false;
        }
        return Arrays.equals((byte[]) src, (byte[]) result);
    }

    @Override
    public boolean compareRoundtripResults(Object src, Object result)
    {
        if (src == null && result == null)
        {
            return true;
        }
        if (src == null || result == null)
        {
            return false;
        }
        return src.equals(result);
    }
}
