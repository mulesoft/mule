/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.transformers;

import java.util.Arrays;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.simple.ByteArrayToHexString;
import org.mule.transformers.simple.HexStringToByteArray;
import org.mule.umo.transformer.UMOTransformer;

/**
 * @author <a href="mailto:holger@codehaus.org">Holger Hoffstaette</a>
 * @version $Revision$
 */
public class HexStringByteArrayTransformersTestCase extends AbstractTransformerTestCase
{
    public UMOTransformer getTransformer() throws Exception
    {
        return new HexStringToByteArray();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
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
    public void testUppercase() throws Exception
    {
        ByteArrayToHexString t = new ByteArrayToHexString();
        t.setUpperCase(true);

        assertEquals(((String)getTestData()).toUpperCase(), t.transform(getResultData()));
    }

}
