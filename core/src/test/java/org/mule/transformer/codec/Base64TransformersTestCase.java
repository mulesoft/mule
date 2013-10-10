/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
