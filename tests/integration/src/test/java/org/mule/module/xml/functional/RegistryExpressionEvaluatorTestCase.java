/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.expression.RegistryExpressionEvaluator;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.FruitBasket;

public class RegistryExpressionEvaluatorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/registry-expressions-test-config.xml";
    }

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

        try
        {
            o = eval.evaluate("bowlToBasket.returnClass.xname", message);
            fail("property xname is not valid and is not optional");
        }
        catch (Exception e)
        {
            //Exprected
        }

    }

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

}