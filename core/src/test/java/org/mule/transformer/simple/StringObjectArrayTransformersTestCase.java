/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
