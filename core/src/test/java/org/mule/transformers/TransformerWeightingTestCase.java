/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.transformers.simple.ObjectToByteArray;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.transformer.TransformerException;

import java.io.FilterInputStream;
import java.io.IOException;

public class TransformerWeightingTestCase extends AbstractMuleTestCase
{

    public void testExactMatch() throws Exception
    {

        DummyTransformer trans = new DummyTransformer();
        trans.setReturnClass(byte[].class);
        trans.registerSourceType(IOException.class);

        TransformerWeighting weighting = new TransformerWeighting(IOException.class, byte[].class, trans);

        assertFalse(weighting.isNotMatch());
        assertTrue(weighting.isExactMatch());
    }

    public void testNearMatch() throws Exception
    {
        ObjectToByteArray trans = new ObjectToByteArray();

        TransformerWeighting weighting = new TransformerWeighting(FilterInputStream.class, byte[].class, trans);

        assertFalse(weighting.isNotMatch());
        assertFalse(weighting.isExactMatch());
        assertEquals(1, weighting.getInputWeighting());
        assertEquals(0, weighting.getOutputWeighting());

    }

    public void testNoMatchWeighting() throws Exception
    {
        SerializableToByteArray trans = new SerializableToByteArray();

        TransformerWeighting weighting = new TransformerWeighting(FruitBowl.class, byte[].class, trans);

        assertTrue(weighting.isNotMatch());
        assertEquals(-1, weighting.getInputWeighting());
        assertEquals(0, weighting.getOutputWeighting());

    }

    public void testCompareWeightingWithNearMatches() throws Exception
    {
        ObjectToByteArray trans1 = new ObjectToByteArray();

        DummyTransformer trans2 = new DummyTransformer();
        trans2.setReturnClass(byte[].class);
        trans2.registerSourceType(Exception.class);

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

        assertEquals(2, weighting1.getInputWeighting());
        assertEquals(1, weighting2.getInputWeighting());
        assertEquals(0, weighting1.getOutputWeighting());
        assertEquals(0, weighting2.getOutputWeighting());

    }

    public void testCompareWeightingWithExactMatch() throws Exception
    {
        ObjectToByteArray trans1 = new ObjectToByteArray();

        DummyTransformer trans2 = new DummyTransformer();
        trans2.setReturnClass(byte[].class);
        trans2.registerSourceType(IOException.class);

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

        assertEquals(2, weighting1.getInputWeighting());
        assertEquals(0, weighting2.getInputWeighting());
        assertEquals(0, weighting1.getOutputWeighting());
        assertEquals(0, weighting2.getOutputWeighting());

    }

    public void testCompareWeightingWithNoMatch() throws Exception
    {
        ObjectToByteArray trans1 = new ObjectToByteArray();

        DummyTransformer trans2 = new DummyTransformer();
        trans2.setReturnClass(byte[].class);
        trans2.registerSourceType(FruitBowl.class);

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

        assertEquals(2, weighting1.getInputWeighting());
        assertEquals(-1, weighting2.getInputWeighting());
        assertEquals(0, weighting1.getOutputWeighting());
        assertEquals(0, weighting2.getOutputWeighting());

    }

    private class DummyTransformer extends AbstractTransformer
    {
        protected Object doTransform(Object src, String encoding) throws TransformerException
        {
            return src;
        }
    }
}
