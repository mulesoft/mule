/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.filters;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.module.ognl.expression.OgnlExpressionEvaluator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class OgnlExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    private MuleContext muleContext = mock(MuleContext.class);

    @Test
    public void testWithExpressions()
    {
        Apple apple = new Apple();
        apple.wash();
        Banana banana = new Banana();
        banana.bite();
        FruitBowl payload = new FruitBowl(apple, banana);
        DefaultMuleMessage msg = new DefaultMuleMessage(payload, muleContext);
        OgnlExpressionEvaluator e = new OgnlExpressionEvaluator();
        Object value = e.evaluate("apple.washed", msg);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue(((Boolean) value).booleanValue());

        value = e.evaluate("bar", msg);
        assertNull(value);
    }
}
