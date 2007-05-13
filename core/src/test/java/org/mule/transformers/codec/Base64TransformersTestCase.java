/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.codec;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.Base64;

public class Base64TransformersTestCase extends AbstractTransformerTestCase
{

    public Object getResultData()
    {
        try
        {
            return Base64.encodeBytes(getTestData().toString().getBytes());
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public Object getTestData()
    {
        return "the quick brown fox jumped over the lazy dog";
    }

    public UMOTransformer getTransformer()
    {
        return new Base64Encoder();
    }

    public UMOTransformer getRoundTripTransformer()
    {
        UMOTransformer t = new Base64Decoder();
        // our input is a String so we expect a String as output
        t.setReturnClass(this.getTestData().getClass());
        return t;
    }

}
