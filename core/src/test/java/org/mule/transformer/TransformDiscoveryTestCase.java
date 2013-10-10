/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer;

import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.RedApple;
import org.mule.transformer.types.DataTypeFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TransformDiscoveryTestCase extends AbstractMuleContextTestCase
{
    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerTransformer(new StringToApple());
        muleContext.getRegistry().registerTransformer(new StringToOrange());
    }

    @Test
    public void testSimpleDiscovery() throws Exception
    {
        Transformer t = muleContext.getRegistry().lookupTransformer(DataTypeFactory.STRING, DataTypeFactory.create(Apple.class));
        assertNotNull(t);
        assertEquals(StringToApple.class, t.getClass());

        t = muleContext.getRegistry().lookupTransformer(DataTypeFactory.STRING, DataTypeFactory.create(Orange.class));
        assertNotNull(t);
        assertEquals(StringToOrange.class, t.getClass());


        try
        {
            muleContext.getRegistry().lookupTransformer(DataTypeFactory.STRING, DataTypeFactory.create(Banana.class));
            fail("There is no transformer to go from String to Banana");
        }
        catch (TransformerException e)
        {
            //expected
        }


        muleContext.getRegistry().registerTransformer(new StringToRedApple());

        t = muleContext.getRegistry().lookupTransformer(DataTypeFactory.STRING, DataTypeFactory.create(RedApple.class));
        assertNotNull(t);
        assertEquals(StringToRedApple.class, t.getClass());
    }

    protected class StringToApple extends org.mule.transformer.AbstractDiscoverableTransformer
    {
        public StringToApple()
        {
            setReturnDataType(DataTypeFactory.create(Apple.class));
        }

        protected Object doTransform(Object src, String encoding) throws TransformerException
        {
            return new Apple();
        }
    }

    protected class StringToRedApple extends org.mule.transformer.AbstractDiscoverableTransformer
    {
        public StringToRedApple()
        {
            setReturnDataType(DataTypeFactory.create(RedApple.class));
            setPriorityWeighting(MAX_PRIORITY_WEIGHTING);
        }

        protected Object doTransform(Object src, String encoding) throws TransformerException
        {
            return new RedApple();
        }
    }

    protected class StringToOrange extends org.mule.transformer.AbstractDiscoverableTransformer
    {
        public StringToOrange()
        {
            setReturnDataType(DataTypeFactory.create(Orange.class));
        }

        protected Object doTransform(Object src, String encoding) throws TransformerException
        {
            return new Orange();
        }
    }
}
