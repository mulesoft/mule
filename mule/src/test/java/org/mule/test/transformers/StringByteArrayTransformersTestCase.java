/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.transformers;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.simple.ByteArrayToString;
import org.mule.transformers.simple.StringToByteArray;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Arrays;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class StringByteArrayTransformersTestCase extends AbstractTransformerTestCase
{
    public UMOTransformer getTransformer() throws Exception
    {
        return new StringToByteArray();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new ByteArrayToString();
    }

    public Object getTestData()
    {
        return "Test";
    }

    public Object getResultData()
    {
        return "Test".getBytes();
    }

    public boolean compareResults(Object src, Object result)
    {
        if (src == null && result == null) {
            return true;
        }
        if (src == null || result == null) {
            return false;
        }
        return Arrays.equals((byte[]) src, (byte[]) result);
    }

    public boolean compareRoundtripResults(Object src, Object result)
    {
        if (src == null && result == null) {
            return true;
        }
        if (src == null || result == null) {
            return false;
        }
        return src.equals(result);
    }
}
