/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.DefaultMuleMessage;
import org.mule.RegistryContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.expression.transformers.BeanBuilderTransformer;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.expression.transformers.ExpressionTransformer;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBowl;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ExpressionTransformerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/transformers/expression-transformers-test.xml";
    }

    public void testTransformerConfig() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) RegistryContext.getRegistry().lookupTransformer("testTransformer");
        assertNotNull(transformer);
        assertNotNull(transformer.getArguments());
        assertEquals(2, transformer.getArguments().size());
        ExpressionArgument arg1 = transformer.getArguments().get(0);
        assertEquals("payload", arg1.getEvaluator());
        assertEquals("org.mule.tck.testmodels.fruit.FruitBasket", arg1.getExpression());
        assertFalse(arg1.isOptional());

        ExpressionArgument arg2 = transformer.getArguments().get(1);
        assertEquals("headers", arg2.getEvaluator());
        assertEquals("foo,bar*", arg2.getExpression());
        assertTrue(arg2.isOptional());

    }

    public void testBeanBuilderTransformerConfig() throws Exception
    {
        BeanBuilderTransformer transformer = (BeanBuilderTransformer) RegistryContext.getRegistry().lookupTransformer("testTransformer3");
        assertNotNull(transformer);
        assertNotNull(transformer.getArguments());
        assertEquals(3, transformer.getArguments().size());
        ExpressionArgument arg1 = transformer.getArguments().get(0);
        assertEquals("brand", arg1.getName());
        assertEquals("mule", arg1.getEvaluator());
        assertEquals("message.payload", arg1.getExpression());
        assertFalse(arg1.isOptional());

        ExpressionArgument arg2 = transformer.getArguments().get(1);
        assertEquals("segments", arg2.getName());
        assertEquals("mule", arg2.getEvaluator());
        assertEquals("message.header(SEGMENTS)", arg2.getExpression());
        assertTrue(arg2.isOptional());

    }

    public void testExecutionWithCorrectMessage() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) RegistryContext.getRegistry().lookupTransformer("testTransformer");
        Properties props = new Properties();
        props.setProperty("foo", "moo");
        props.setProperty("bar", "mar");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props);

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        Object o1 = ((Object[]) result)[0];
        assertTrue(o1 instanceof FruitBasket);

        Object o2 = ((Object[]) result)[1];
        assertTrue(o2 instanceof Map);
        assertEquals(2, ((Map) o2).size());
        assertEquals("moo", ((Map) o2).get("foo"));
        assertEquals("mar", ((Map) o2).get("bar"));
    }

    public void testExecutionWithPartialMissingOptionalParams() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) RegistryContext.getRegistry().lookupTransformer("testTransformer");
        Properties props = new Properties();
        props.setProperty("foo", "moo");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props);

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        Object o1 = ((Object[]) result)[0];
        assertTrue(o1 instanceof FruitBasket);

        Object o2 = ((Object[]) result)[1];
        assertTrue(o2 instanceof Map);
        assertEquals(1, ((Map) o2).size());
        assertEquals("moo", ((Map) o2).get("foo"));

    }

    public void testExecutionWithAllMissingOptionalParams() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) RegistryContext.getRegistry().lookupTransformer("testTransformer");
        Properties props = new Properties();

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props);

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        Object o1 = ((Object[]) result)[0];
        assertTrue(o1 instanceof FruitBasket);

        assertNull(((Object[]) result)[1]);
    }

    public void testTransformerConfigWithSingleArgument() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) RegistryContext.getRegistry().lookupTransformer("testTransformer2");
        Properties props = new Properties();
        props.setProperty("foo", "moo");
        props.setProperty("bar", "mar");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props);

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertFalse(result.getClass().isArray());
        assertTrue(result instanceof List);
        assertTrue(((List)result).contains("moo"));
        assertTrue(((List)result).contains("mar"));
    }


    public void testExecutionWithInCorrectMessage() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) RegistryContext.getRegistry().lookupTransformer("testTransformer2");
        Properties props = new Properties();
        props.setProperty("foo", "moo");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props);

        try
        {
            transformer.transform(message);
            fail("Not all headers present, the transform should have failed");
        }
        catch (TransformerException e)
        {
            //exprected
        }

    }
}