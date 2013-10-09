/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
