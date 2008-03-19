/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.codec;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.util.Base64;

public class Base64TransformersTestCase extends AbstractTransformerTestCase
{
    private static final String TEST_DATA = "the quick brown fox jumped over the lazy dog";
    
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

    public Object getTestData()
    {
        return TEST_DATA;
    }

    public Transformer getTransformer()
    {
        return new Base64Encoder();
    }

    public Transformer getRoundTripTransformer()
    {
        Transformer t = new Base64Decoder();
        // our input is a String so we expect a String as output
        t.setReturnClass(String.class);
        return t;
    }
    
}
