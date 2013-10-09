/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.codec;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.Base64;

import static org.junit.Assert.fail;

public class Base64TransformersTestCase extends AbstractTransformerTestCase
{
    private static final String TEST_DATA = "the quick brown fox jumped over the lazy dog";
    
    @Override
    public Object getResultData()
    {
        try
        {
            return Base64.encodeBytes(TEST_DATA.getBytes());
        }
        catch (Exception ex)
        {
            fail();
            return null;
        }
    }

    @Override
    public Object getTestData()
    {
        return TEST_DATA;
    }

    @Override
    public Transformer getTransformer()
    {
        return new Base64Encoder();
    }

    @Override
    public Transformer getRoundTripTransformer()
    {
        Transformer t = new Base64Decoder();
        // our input is a String so we expect a String as output
        t.setReturnDataType(DataTypeFactory.STRING);
        return t;
    }
}
