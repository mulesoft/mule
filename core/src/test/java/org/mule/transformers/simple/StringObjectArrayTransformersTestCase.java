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
import org.mule.umo.transformer.UMOTransformer;

public class StringObjectArrayTransformersTestCase extends AbstractTransformerTestCase
{

    public UMOTransformer getTransformer() throws Exception
    {
        return new StringToObjectArray();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
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

    public boolean compareResults(Object src, Object result)
    {
        if (src == null || result == null)
        {
            return false;
        }

        if (result instanceof Object[])
        {
            Object[] out = (Object[])result;
            assertEquals(out[0].toString(), "test1");
            assertEquals(out[1].toString(), "test2");
            assertEquals(out[2].toString(), "test3");
            return true;
        }

        return false;
    }

    public boolean compareRoundtripResults(Object src, Object result)
    {
        if (src == null || result == null)
        {
            return false;
        }
        return src.equals(result);
    }
}

