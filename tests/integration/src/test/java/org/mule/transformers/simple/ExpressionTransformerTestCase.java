/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.construct.Flow;
import org.mule.expression.transformers.BeanBuilderTransformer;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.expression.transformers.ExpressionTransformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBowl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ExpressionTransformerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/transformers/expression-transformers-test.xml";
    }

    private void testTransformerConfig(String transformerName) throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry().lookupTransformer(transformerName);
        assertNotNull(transformer);
        assertNotNull(transformer.getArguments());
        assertEquals(2, transformer.getArguments().size());
        ExpressionArgument arg1 = transformer.getArguments().get(0);
        assertEquals("payload", arg1.getEvaluator());
        assertEquals("org.mule.tck.testmodels.fruit.FruitBasket", arg1.getExpression());
        assertFalse(arg1.isOptional());

        ExpressionArgument arg2 = transformer.getArguments().get(1);
        assertEquals("headers", arg2.getEvaluator());
        assertEquals("foo,bar?", arg2.getExpression());
        assertTrue(arg2.isOptional());
    }

    @Test
    public void testTransformerConfig() throws Exception
    {
        testTransformerConfig("testTransformer");
    }

    @Test
    public void testTransformerConfigEvaluatorInline() throws Exception
    {
        testTransformerConfig("testTransformerInline");
    }

    @Test
    public void testBeanBuilderTransformerConfig() throws Exception
    {
        testBeanBuilderTransformerConfig("testTransformer3");
    }

    @Test
    public void testBeanBuilderTransformerConfigEvaluatorInline() throws Exception
    {
        testBeanBuilderTransformerConfig("testTransformer3Inline");
    }

    private void testBeanBuilderTransformerConfig(String transformerName) throws Exception
    {
        BeanBuilderTransformer transformer = (BeanBuilderTransformer) muleContext.getRegistry().lookupTransformer(transformerName);
        assertNotNull(transformer);
        assertNotNull(transformer.getArguments());
        assertEquals(3, transformer.getArguments().size());
        ExpressionArgument arg1 = transformer.getArguments().get(0);
        assertEquals("brand", arg1.getName());
        assertEquals("message", arg1.getEvaluator());
        assertEquals("payload", arg1.getExpression());
        assertFalse(arg1.isOptional());

        ExpressionArgument arg2 = transformer.getArguments().get(1);
        assertEquals("segments", arg2.getName());
        assertEquals("header", arg2.getEvaluator());
        assertEquals("SEGMENTS", arg2.getExpression());
        assertTrue(arg2.isOptional());
    }

    @Test
    public void testExecutionWithCorrectMessage() throws Exception
    {
        testExecutionWithCorrectMessage("testTransformer");
    }

    @Test
    public void testExecutionWithCorrectMessageEvaluatorInline() throws Exception
    {
        testExecutionWithCorrectMessage("testTransformerInline");
    }

    private void testExecutionWithCorrectMessage(String transformerName) throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry().lookupTransformer(transformerName);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "moo");
        props.put("bar", "mar");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props, muleContext);

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
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry().lookupTransformer("testTransformer");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "moo");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props, muleContext);

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        Object o1 = ((Object[]) result)[0];
        assertTrue(o1 instanceof FruitBasket);

        Object o2 = ((Object[]) result)[1];
        assertTrue(o2 instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) o2;
        assertEquals(1, map.size());
        assertEquals("moo", map.get("foo"));
    }

    @Test
    public void testExecutionWithAllMissingOptionalParams() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry().lookupTransformer("testTransformer");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), muleContext);

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        Object o1 = ((Object[]) result)[0];
        assertTrue(o1 instanceof FruitBasket);

        assertNull(((Object[]) result)[1]);
    }

    @Test
    public void testTransformerConfigWithSingleArgument() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry().lookupTransformer("testTransformer2");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "moo");
        props.put("bar", "mar");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props, muleContext);

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

    @Test
    public void testTransformerConfigWithSingleArgumentShortcutConfigEvaluatorInline() throws Exception
    {
        testTransformerConfigWithSingleArgumentShortcutConfig("testTransformer4Inline");
    }

    private void testTransformerConfigWithSingleArgumentShortcutConfig(String transformerName) throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry().lookupTransformer(transformerName);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "moo");
        props.put("bar", "mar");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props, muleContext);

        Object result = transformer.transform(message);
        assertNotNull(result);
        assertFalse(result.getClass().isArray());
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertTrue(list.contains("moo"));
        assertTrue(list.contains("mar"));
    }

    @Test
    public void testTransformerConfigWithSingleArgumentShortcutConfigInFlow() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("et");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "moo");
        props.put("bar", "mar");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props,
            muleContext);

        MuleEvent resultEvent = flow.process(new DefaultMuleEvent(message, getTestInboundEndpoint(""),
            getTestService(), getTestSession(null, muleContext)));
        assertNotNull(resultEvent);
        assertNotNull(resultEvent.getMessage().getPayload());
        Object payload = resultEvent.getMessage().getPayload();
        assertFalse(payload.getClass().isArray());
        assertTrue(payload instanceof List<?>);
        List<?> list = (List<?>) payload;
        assertTrue(list.contains("moo"));
        assertTrue(list.contains("mar"));
    }

    @Test(expected = RequiredValueException.class)
    public void testExecutionWithInCorrectMessage() throws Exception
    {
        ExpressionTransformer transformer = (ExpressionTransformer) muleContext.getRegistry().lookupTransformer("testTransformer2");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "moo");

        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), props, muleContext);

        transformer.transform(message);
        fail("Not all headers present, the transform should have failed");
    }
}
