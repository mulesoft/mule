/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;

import static org.junit.Assert.assertEquals;

public class StringObjectArrayTransformersTestCase extends AbstractTransformerTestCase
{

    public Transformer getTransformer() throws Exception
    {
        return new StringToObjectArray();
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        return new ObjectArrayToString();
    }

    public Object getTestData()
    {
        return "test1 test2 test3";
    }

    public Object getResultData()
    {
        return new String[]{"test1", "test2", "test3"};
    }

    @Override
    public boolean compareResults(Object src, Object result)
    {
        if (src == null || result == null)
        {
            return false;
        }

        if (result instanceof Object[])
        {
            Object[] out = (Object[]) result;
            assertEquals(out[0].toString(), "test1");
            assertEquals(out[1].toString(), "test2");
            assertEquals(out[2].toString(), "test3");
            return true;
        }

        return false;
    }

    @Override
    public boolean compareRoundtripResults(Object src, Object result)
    {
        if (src == null || result == null)
        {
            return false;
        }
        return src.equals(result);
    }

}
