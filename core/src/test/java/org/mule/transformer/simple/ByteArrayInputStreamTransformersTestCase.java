/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;

import java.io.ByteArrayInputStream;

public class ByteArrayInputStreamTransformersTestCase extends AbstractTransformerTestCase
{

    public Transformer getTransformer() throws Exception
    {
        return new ObjectToInputStream();
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        return new ObjectToByteArray();
    }

    public Object getTestData()
    {
        return TEST_MESSAGE.getBytes();
    }

    public Object getResultData()
    {
        return new ByteArrayInputStream(TEST_MESSAGE.getBytes());
    }

}
