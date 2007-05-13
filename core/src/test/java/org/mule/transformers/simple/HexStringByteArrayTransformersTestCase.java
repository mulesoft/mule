/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Arrays;

public class HexStringByteArrayTransformersTestCase extends AbstractTransformerTestCase
{

    public UMOTransformer getTransformer()
    {
        return new HexStringToByteArray();
    }

    public UMOTransformer getRoundTripTransformer()
    {
        return new ByteArrayToHexString();
    }

    public Object getTestData()
    {
        return "01020aff";
    }

    public Object getResultData()
    {
        return new byte[]{1, 2, 10, (byte)0xff};
    }

    // @Override
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
        return Arrays.equals((byte[])src, (byte[])result);
    }

    // @Override
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

    // extra test for uppercase output
    public void testUppercase() throws TransformerException
    {
        ByteArrayToHexString t = new ByteArrayToHexString();
        t.setUpperCase(true);

        assertEquals(((String)getTestData()).toUpperCase(), t.transform(getResultData()));
    }

}
