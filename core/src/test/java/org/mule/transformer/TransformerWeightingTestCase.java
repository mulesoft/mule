/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.BloodOrange;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.SerializableToByteArray;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TransformerWeightingTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testExactMatch() throws Exception
    {
        DummyTransformer trans = new DummyTransformer();
        trans.setReturnDataType(DataTypeFactory.BYTE_ARRAY);
        trans.registerSourceType(DataTypeFactory.create(IOException.class));

        TransformerWeighting weighting = new TransformerWeighting(IOException.class, byte[].class, trans);

        assertFalse(weighting.isNotMatch());
        assertTrue(weighting.isExactMatch());
    }

    @Test
    public void testNearMatch() throws Exception
    {
        ObjectToByteArray trans = new ObjectToByteArray();

        TransformerWeighting weighting = new TransformerWeighting(FilterInputStream.class, byte[].class, trans);

        assertFalse(weighting.isNotMatch());
        assertFalse(weighting.isExactMatch());
        assertEquals(1, weighting.getInputWeighting());
        assertEquals(0, weighting.getOutputWeighting());

    }

    @Test
    public void testNoMatchWeighting() throws Exception
    {
        SerializableToByteArray trans = new SerializableToByteArray();

        TransformerWeighting weighting = new TransformerWeighting(FruitBowl.class, byte[].class, trans);

        assertTrue(weighting.isNotMatch());
        assertEquals(-1, weighting.getInputWeighting());
        assertEquals(0, weighting.getOutputWeighting());

    }

    @Test
    public void testCompareWeightingWithNearMatches() throws Exception
    {
        ObjectToByteArray trans1 = new ObjectToByteArray();

        DummyTransformer trans2 = new DummyTransformer();
        trans2.setReturnDataType(DataTypeFactory.BYTE_ARRAY);
        trans2.registerSourceType(DataTypeFactory.create(Exception.class));

        TransformerWeighting weighting1 =
                new TransformerWeighting(IOException.class, byte[].class, trans1);
        TransformerWeighting weighting2 =
                new TransformerWeighting(IOException.class, byte[].class, trans2);

        assertFalse(weighting1.isNotMatch());
        assertFalse(weighting2.isNotMatch());
        assertFalse(weighting1.isExactMatch());
        assertFalse(weighting2.isExactMatch());
        //Weighting2 two is a better match
        assertEquals(1, weighting2.compareTo(weighting1));

        assertEquals(3, weighting1.getInputWeighting());
        assertEquals(1, weighting2.getInputWeighting());
        assertEquals(0, weighting1.getOutputWeighting());
        assertEquals(0, weighting2.getOutputWeighting());

    }

    @Test
    public void testCompareWeightingWithExactMatch() throws Exception
    {
        ObjectToByteArray trans1 = new ObjectToByteArray();

        DummyTransformer trans2 = new DummyTransformer();
        trans2.setReturnDataType(DataTypeFactory.BYTE_ARRAY);
        trans2.registerSourceType(DataTypeFactory.create(IOException.class));

        TransformerWeighting weighting1 =
                new TransformerWeighting(IOException.class, byte[].class, trans1);
        TransformerWeighting weighting2 =
                new TransformerWeighting(IOException.class, byte[].class, trans2);

        assertFalse(weighting1.isNotMatch());
        assertFalse(weighting2.isNotMatch());
        assertFalse(weighting1.isExactMatch());
        assertTrue(weighting2.isExactMatch());
        //Weighting2 two is an exact match
        assertEquals(1, weighting2.compareTo(weighting1));

        assertEquals(3, weighting1.getInputWeighting());
        assertEquals(0, weighting2.getInputWeighting());
        assertEquals(0, weighting1.getOutputWeighting());
        assertEquals(0, weighting2.getOutputWeighting());

    }

    @Test
    public void testCompareWeightingWithNoMatch() throws Exception
    {
        ObjectToByteArray trans1 = new ObjectToByteArray();

        DummyTransformer trans2 = new DummyTransformer();
        trans2.setReturnDataType(DataTypeFactory.BYTE_ARRAY);
        trans2.registerSourceType(DataTypeFactory.create(FruitBowl.class));

        TransformerWeighting weighting1 =
                new TransformerWeighting(IOException.class, byte[].class, trans1);
        TransformerWeighting weighting2 =
                new TransformerWeighting(IOException.class, byte[].class, trans2);

        assertFalse(weighting1.isNotMatch());
        assertTrue(weighting2.isNotMatch());
        assertFalse(weighting1.isExactMatch());
        assertFalse(weighting2.isExactMatch());
        //Weighting2 two is not a match
        assertEquals(-1, weighting2.compareTo(weighting1));

        assertEquals(3, weighting1.getInputWeighting());
        assertEquals(-1, weighting2.getInputWeighting());
        assertEquals(0, weighting1.getOutputWeighting());
        assertEquals(0, weighting2.getOutputWeighting());

    }

    @Test
    public void testPriorityMatching() throws Exception
    {
        DummyTransformer t1 = new DummyTransformer();
        t1.setName("--t1");
        t1.registerSourceType(DataTypeFactory.create(Orange.class));
        t1.setReturnDataType(DataTypeFactory.create(Fruit.class));
        muleContext.getRegistry().registerTransformer(t1);

        DummyTransformer t2 = new DummyTransformer();
        t2.setName("--t2");
        t2.registerSourceType(DataTypeFactory.OBJECT);
        t2.setReturnDataType(DataTypeFactory.create(Fruit.class));
        muleContext.getRegistry().registerTransformer(t2);

        List trans = muleContext.getRegistry().lookupTransformers(new SimpleDataType(BloodOrange.class), new SimpleDataType(Fruit.class));
        assertEquals(2, trans.size());
        for (Iterator iterator = trans.iterator(); iterator.hasNext();)
        {
            Transformer transformer = (Transformer) iterator.next();
            assertTrue(transformer.getName().startsWith("--"));
        }

        Transformer result = muleContext.getRegistry().lookupTransformer(new SimpleDataType(BloodOrange.class), new SimpleDataType(Fruit.class));
        assertNotNull(result);
        assertEquals("--t1", result.getName());
    }

    private static class DummyTransformer extends AbstractTransformer implements DiscoverableTransformer
    {
        private int weighting;

        public DummyTransformer()
        {
            super();
        }
        
        public int getPriorityWeighting()
        {
            return weighting;
        }


        public void setPriorityWeighting(int weighting)
        {
            this.weighting = weighting;
        }

        @Override
        protected Object doTransform(Object src, String encoding) throws TransformerException
        {
            return src;
        }
    }
}
