/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

import java.io.FilterInputStream;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TransformerCachingTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testCacheUpdate() throws Exception
    {
        SimpleDataType<?> sourceType = new SimpleDataType<FilterInputStream>(FilterInputStream.class);
        Transformer trans = muleContext.getRegistry().lookupTransformer(sourceType, DataTypeFactory.BYTE_ARRAY);
        assertNotNull(trans);
        assertTrue(trans instanceof ObjectToByteArray);

        Transformer trans2 = new FilterInputStreamToByteArray();
        muleContext.getRegistry().registerTransformer(trans2);

        trans = muleContext.getRegistry().lookupTransformer(sourceType, DataTypeFactory.BYTE_ARRAY);
        assertNotNull(trans);
        assertTrue(trans instanceof FilterInputStreamToByteArray);

        trans = muleContext.getRegistry().lookupTransformer(DataTypeFactory.INPUT_STREAM, DataTypeFactory.BYTE_ARRAY);
        assertNotNull(trans);
        assertTrue(trans instanceof ObjectToByteArray);

        muleContext.getRegistry().unregisterTransformer(trans2.getName());

        trans = muleContext.getRegistry().lookupTransformer(sourceType, DataTypeFactory.BYTE_ARRAY);
        assertNotNull(trans);
        assertTrue(trans instanceof ObjectToByteArray);
    }

    public static class FilterInputStreamToByteArray extends AbstractTransformer implements DiscoverableTransformer
    {
        public FilterInputStreamToByteArray()
        {
            registerSourceType(DataTypeFactory.create(FilterInputStream.class));
            setReturnDataType(DataTypeFactory.BYTE_ARRAY);
        }

        @Override
        protected Object doTransform(Object src, String outputEncoding) throws TransformerException
        {
            throw new UnsupportedOperationException("This is a transformer only to be used for testing");
        }

        public int getPriorityWeighting()
        {
            return 0;
        }

        public void setPriorityWeighting(int weighting)
        {
            //no-op
        }
    }
}
