/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.SimpleDataType;

import org.junit.Test;

public class GraphTransformerResolutionTestCase extends FunctionalTestCase
{
    public static class A
    {

        private final String value;

        public A(String value)
        {
            this.value = value;
        }
    }

    public static class B
    {

        private final String value;

        public B(String value)
        {
            this.value = value;
        }
    }

    public static class C
    {
        private final String value;

        public C(String value)
        {
            this.value = value;
        }
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/transformers/graph-transformer-resolution-config.xml";
    }

    @Test
    public void resolvesNonDirectTransformation() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testInput", new A("Hello"), null);
        assertTrue(response.getPayload() instanceof C);
        assertEquals("HelloAFromB", ((C)response.getPayload()).value);
    }

    public static class AtoBConverter extends AbstractTransformer implements DiscoverableTransformer
    {
        public AtoBConverter()
        {
            registerSourceType(new SimpleDataType<Object>(A.class));
            setReturnDataType(new SimpleDataType<Object>(B.class));
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return new B(((A) src).value + "A");
        }

        @Override
        public int getPriorityWeighting()
        {
            return 10;
        }

        @Override
        public void setPriorityWeighting(int weighting)
        {
        }
    }

    public static class BtoCConverter extends AbstractTransformer implements DiscoverableTransformer
    {

        public BtoCConverter()
        {
            registerSourceType(new SimpleDataType<Object>(B.class));
            setReturnDataType(new SimpleDataType<Object>(C.class));
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return new C(((B) src).value + "FromB");
        }

        @Override
        public int getPriorityWeighting()
        {
            return 10;
        }

        @Override
        public void setPriorityWeighting(int weighting)
        {
        }
    }
}
