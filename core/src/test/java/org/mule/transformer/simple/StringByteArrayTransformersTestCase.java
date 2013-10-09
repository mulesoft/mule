/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
