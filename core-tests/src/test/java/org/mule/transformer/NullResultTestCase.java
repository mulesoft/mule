/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

import org.junit.Test;

import static org.junit.Assert.fail;

public class NullResultTestCase extends AbstractTransformerTestCase
{
    private final NullResultTransformer transformer = new NullResultTransformer();

    @Override
    public Object getTestData()
    {
        return new Object();
    }

    @Override
    public Object getResultData()
    {
        return NullPayload.getInstance();
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        return transformer;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    @Test
    public void testNullNotExpected() throws Exception
    {
        transformer.setReturnDataType(DataTypeFactory.STRING);
        try
        {
            testTransform();
            fail("Transformer should have thrown an exception because the return class doesn't match the result.");
        }
        catch (TransformerException e)
        {
            // expected
        }
    }

    public static final class NullResultTransformer extends AbstractTransformer
    {
        public NullResultTransformer()
        {
            super();
            this.registerSourceType(DataTypeFactory.OBJECT);
            this.setReturnDataType(DataTypeFactory.create(NullPayload.class));
        }

        @Override
        public Object doTransform(Object src, String encoding) throws TransformerException
        {
            return null;
        }
    }
}
