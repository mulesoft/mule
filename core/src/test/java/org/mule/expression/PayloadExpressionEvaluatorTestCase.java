/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitBowlToFruitBasket;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class PayloadExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testSimple() throws Exception
    {
        MessagePayloadExpressionEvaluator eval = new MessagePayloadExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);

        //no expression
        Object result = eval.evaluate(null, message);
        assertNotNull(result);
        assertEquals("test", result);

        //no expression
        result = eval.evaluate(null, null);
        assertNull(result);
    }

    /**
     * Make sure the evaluator gets registered properly
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testSimpleUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);

        assertFalse(muleContext.getExpressionManager().isValidExpression("${payload:}"));
        assertTrue(muleContext.getExpressionManager().isValidExpression("#[payload:]"));

        Object result = muleContext.getExpressionManager().evaluate("#[payload:]", message);
        assertNotNull(result);
        assertEquals("test", result);

        result = muleContext.getExpressionManager().evaluate("#[payload:]", (MuleMessage) null);
        assertNull(result);
    }

    @Test
    public void testWithTransform() throws Exception
    {
        MessagePayloadExpressionEvaluator eval = new MessagePayloadExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);

        //i.e. ${payload:byte[]}
        Object result = eval.evaluate("byte[]", message);
        assertNotNull(result);
        assertTrue(result instanceof byte[]);
        assertEquals("test", new String((byte[]) result));

        ByteArrayInputStream bais = new ByteArrayInputStream("test2".getBytes());
        //i.e. ${payload:java.lang.String}
        result = eval.evaluate("java.lang.String", new DefaultMuleMessage(bais, muleContext));
        assertNotNull(result);
        assertEquals("test2", result);
    }

    @Test
    public void testWithMoreComplexTransform() throws Exception
    {
        MessagePayloadExpressionEvaluator eval = new MessagePayloadExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), muleContext);

        //Lets register our transformer so Mule can find it
        muleContext.getRegistry().registerTransformer(new FruitBowlToFruitBasket());

        //i.e. ${payload:org.mule.tck.testmodels.fruit.FruitBasket}
        Object result = eval.evaluate("org.mule.tck.testmodels.fruit.FruitBasket", message);
        assertNotNull(result);
        assertTrue(result instanceof FruitBasket);
        FruitBasket fb = (FruitBasket) result;
        assertEquals(2, fb.getFruit().size());
        assertTrue(fb.hasBanana());
        assertTrue(fb.hasApple());
    }
}
