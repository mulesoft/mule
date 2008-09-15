/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitBowlToFruitBasket;
import org.mule.util.expression.ExpressionEvaluatorManager;
import org.mule.util.expression.MessagePayloadExpressionEvaluator;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class PayloadExpressionEvaluatorTestCase extends AbstractMuleTestCase
{
    public void testSimple() throws Exception
    {
        MessagePayloadExpressionEvaluator eval = new MessagePayloadExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage("test");

        //no expression
        Object result = eval.evaluate(null, message);
        assertNotNull(result);
        assertEquals("test", result);

        //no expression
        result = eval.evaluate(null, new ArrayList(1));
        assertNotNull(result);
        assertTrue(result instanceof List);

        result = eval.evaluate(null, null);
        assertNull(result);
    }

    /**
     * Make sure the evaluator gets registered properly
     *
     * @throws Exception if the test fails
     */
    public void testSimpleUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test");

        assertTrue(ExpressionEvaluatorManager.isValidExpression("${payload:}"));

        Object result = ExpressionEvaluatorManager.evaluate("${payload:}", message);
        assertNotNull(result);
        assertEquals("test", result);

        result = ExpressionEvaluatorManager.evaluate("${payload:}", new ArrayList(1));
        assertNotNull(result);
        assertTrue(result instanceof List);

        result = ExpressionEvaluatorManager.evaluate("${payload:}", null);
        assertNull(result);
    }

    public void testWithTransform() throws Exception
    {
        MessagePayloadExpressionEvaluator eval = new MessagePayloadExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage("test");

        //i.e. ${payload:byte[]}
        Object result = eval.evaluate("byte[]", message);
        assertNotNull(result);
        assertTrue(result instanceof byte[]);
        assertEquals("test", new String((byte[]) result));

        ByteArrayInputStream bais = new ByteArrayInputStream("test2".getBytes());
        //i.e. ${payload:java.lang.String}
        result = eval.evaluate("java.lang.String", new DefaultMuleMessage(bais));
        assertNotNull(result);
        assertEquals("test2", result);
    }

    public void testWithMoreComplexTransform() throws Exception
    {
        MessagePayloadExpressionEvaluator eval = new MessagePayloadExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()));

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
