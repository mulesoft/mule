/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.transformers.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.expression.transformers.BeanBuilderTransformer;
import org.mule.runtime.core.expression.transformers.ExpressionArgument;
import org.mule.runtime.core.expression.transformers.ExpressionTransformer;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBowl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ExpressionTransformerELTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/transformers/expression-transformers-el-test.xml";
    }

    private void testTransformerConfig(String name) throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry()
            .lookupTransformer(name);
        assertNotNull(transformer);
        assertNotNull(transformer.getArguments());
        assertEquals(2, transformer.getArguments().size());
        ExpressionArgument arg1 = transformer.getArguments().get(0);
        assertEquals("message.payloadAs(org.mule.tck.testmodels.fruit.FruitBasket)", arg1.getExpression());
        assertFalse(arg1.isOptional());

        ExpressionArgument arg2 = transformer.getArguments().get(1);
        assertEquals("['foo' : message.outboundProperties.foo, 'bar' : message.outboundProperties.bar]", arg2.getExpression());
        assertTrue(arg2.isOptional());
    }

    @Test
    public void testTransformerConfig() throws Exception
    {
        testTransformerConfig("testTransformer");
    }

    @Test
    public void testBeanBuilderTransformerConfig() throws Exception
    {
        testBeanBuilderTransformerConfig("testTransformer3");
    }

    private void testBeanBuilderTransformerConfig(String name) throws Exception
    {
        BeanBuilderTransformer transformer = (BeanBuilderTransformer) muleContext.getRegistry()
            .lookupTransformer(name);
        assertNotNull(transformer);
        assertNotNull(transformer.getArguments());
        assertEquals(3, transformer.getArguments().size());
        ExpressionArgument arg1 = transformer.getArguments().get(0);
        assertEquals("brand", arg1.getName());
        assertEquals("payload", arg1.getExpression());
        assertFalse(arg1.isOptional());

        ExpressionArgument arg2 = transformer.getArguments().get(1);
        assertEquals("segments", arg2.getName());
        assertEquals("message.outboundProperties['SEGMENTS']", arg2.getExpression());
        assertTrue(arg2.isOptional());
    }

    @Test
    public void testExecutionWithCorrectMessage() throws Exception
    {
        testExecutionWithCorrectMessage("testTransformer");
    }

    private void testExecutionWithCorrectMessage(String name) throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry()
            .lookupTransformer(name);
        Map<String, Serializable> props = new HashMap<>();
        props.put("foo", "moo");
        props.put("bar", "mar");

        MuleMessage message = MuleMessage.builder().payload(new FruitBowl(new Apple(), new Banana())).outboundProperties(props).build();

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        Object o1 = ((Object[]) result)[0];
        assertTrue(o1 instanceof FruitBasket);

        Object o2 = ((Object[]) result)[1];
        assertTrue(o2 instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) o2;
        assertEquals(2, map.size());
        assertEquals("moo", map.get("foo"));
        assertEquals("mar", map.get("bar"));
    }

    @Test
    public void testExecutionWithPartialMissingOptionalParams() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry()
            .lookupTransformer("testTransformer");
        Map<String, Serializable> props = new HashMap<>();
        props.put("foo", "moo");

        MuleMessage message = MuleMessage.builder().payload(new FruitBowl(new Apple(), new Banana())).outboundProperties(props).build();

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        Object o1 = ((Object[]) result)[0];
        assertTrue(o1 instanceof FruitBasket);

        Object o2 = ((Object[]) result)[1];
        assertTrue(o2 instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) o2;
        assertEquals(2, map.size());
        assertEquals("moo", map.get("foo"));
    }

    @Test
    public void testTransformerConfigWithSingleArgument() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry()
            .lookupTransformer("testTransformer2");
        Map<String, Serializable> props = new HashMap<>();
        props.put("foo", "moo");
        props.put("bar", "mar");

        MuleMessage message = MuleMessage.builder().payload(new FruitBowl(new Apple(), new Banana())).outboundProperties(props).build();

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertFalse(result.getClass().isArray());
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertTrue(list.contains("moo"));
        assertTrue(list.contains("mar"));
    }

    @Test
    public void testTransformerConfigWithSingleArgumentShortcutConfig() throws Exception
    {
        testTransformerConfigWithSingleArgumentShortcutConfig("testTransformer4");
    }

    private void testTransformerConfigWithSingleArgumentShortcutConfig(String name) throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry()
            .lookupTransformer(name);
        Map<String, Serializable> props = new HashMap<>();
        props.put("foo", "moo");
        props.put("bar", "mar");

        MuleMessage message = MuleMessage.builder().payload(new FruitBowl(new Apple(), new Banana())).outboundProperties(props).build();

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertFalse(result.getClass().isArray());
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertTrue(list.contains("moo"));
        assertTrue(list.contains("mar"));
    }

}
