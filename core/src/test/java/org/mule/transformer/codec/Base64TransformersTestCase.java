/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.codec;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.Base64;

import org.junit.Test;

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

    @Test
    public void decodeUnpaddedString() throws Exception
    {
        String encodeBytes = (String) getResultData();
        assertThat(encodeBytes, endsWith("="));
        while (encodeBytes.endsWith("="))
        {
            encodeBytes = encodeBytes.substring(0, encodeBytes.length() - 1);
        }
        assertThat(encodeBytes, not(endsWith("=")));

        String resultString = (String) getRoundTripTransformer().transform(encodeBytes);

        assertThat(resultString, is(TEST_DATA));
    }
}
