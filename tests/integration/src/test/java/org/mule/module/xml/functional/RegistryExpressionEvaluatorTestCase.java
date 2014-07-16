/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.transformer.Transformer;
import org.mule.expression.RegistryExpressionEvaluator;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.FruitBasket;

import org.junit.Test;

public class RegistryExpressionEvaluatorTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/registry-expressions-test-config.xml";
    }

    @Test
    public void testSimpleRegistryLookup() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple(), muleContext);
        RegistryExpressionEvaluator eval = new RegistryExpressionEvaluator();
        eval.setMuleContext(muleContext);
        Object o = eval.evaluate("bowlToBasket", message);
        assertNotNull(o);
        assertTrue(o instanceof Transformer);

        o = eval.evaluate("XXbowlToBasket*", message);
        assertNull(o);
    }

    @Test(expected = ExpressionRuntimeException.class)
    public void testRegistryLookupWithProperties() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple(), muleContext);
        RegistryExpressionEvaluator eval = new RegistryExpressionEvaluator();
        eval.setMuleContext(muleContext);
        Object o = eval.evaluate("bowlToBasket.returnClass", message);
        assertNotNull(o);
        assertTrue(o instanceof Class);
        assertEquals(FruitBasket.class, o);

        o = eval.evaluate("XXbowlToBasket*.returnClass", message);
        assertNull(o);

        o = eval.evaluate("bowlToBasket.returnClass.name", message);
        assertNotNull(o);
        assertEquals(FruitBasket.class.getName(), o);

        o = eval.evaluate("bowlToBasket.returnClass.xname*", message);
        assertNull(o);


        o = eval.evaluate("bowlToBasket.returnClass.xname", message);
    }

    @Test
    public void testGlobalEndpointRegistryLookupWithProperties() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple(), muleContext);
        RegistryExpressionEvaluator eval = new RegistryExpressionEvaluator();
        eval.setMuleContext(muleContext);
        Object o = eval.evaluate("myendpoint.toString", message);
        assertNotNull(o);
        assertTrue(o instanceof String);
        assertEquals("test://foo", o);

        //This doesn't work yet
//        o = eval.evaluate("myendpoint.properties", message);
//        assertNotNull(o);
//        assertTrue(o instanceof Map);
//        assertEquals(2, ((Map)o).size());
//
//        o = eval.evaluate("myendpoint.properties.foo", message);
//        assertNotNull(o);
//        assertEquals("foo-value", o);

    }

    @Test(expected = ExpressionRuntimeException.class)
    public void testLookUpbyType() throws Exception
    {
        Apple apple = new Apple();
        muleContext.getRegistry().registerObject("apple", apple);
        MuleMessage message = new DefaultMuleMessage("foo", muleContext);
        RegistryExpressionEvaluator eval = new RegistryExpressionEvaluator();
        eval.setMuleContext(muleContext);
        Object o = eval.evaluate("type:org.mule.tck.testmodels.fruit.Apple", message);
        assertNotNull(o);
        assertEquals(apple, o);

        o = eval.evaluate("banana", message);
    }

}
