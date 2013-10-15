/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.config;

import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.model.InvocationResult;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JsonPathAnnotatedEntryPointResolverTestCase extends AbstractMuleContextTestCase
{
    public static final String TEST_PAYLOAD = "{\"foo\" : {\"bar\" : [4, 8] }}";

    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerObject("primitives" , new PrimitveTransformers());
    }

    @Test
    public void testAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent component = new AnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        //Since AnnotatedComponent2 has two annotated methods we need to set the method to call
        context.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "doStuff", PropertyScope.INVOCATION);
        InvocationResult result = resolver.invoke(component, context);
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
        assertTrue(result.getResult() instanceof Map);
        Map<?, ?> map = (Map<?, ?>)result.getResult();
        assertEquals(3, map.size());
        assertTrue(map.get("foo") instanceof JsonNode);
        assertTrue((Boolean)map.get("isBarValue"));
        assertEquals("4", map.get("bar"));
    }

    @Test
    public void testAnnotatedMethod2() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent component = new AnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        //Since AnnotatedComponent2 has two annotated methods we need to set the method to call
        context.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "doStuff2", PropertyScope.INVOCATION);
        InvocationResult result = resolver.invoke(component, context);
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
        assertTrue(result.getResult() instanceof Map);
        Map<?, ?> map = (Map<?, ?>)result.getResult();
        assertEquals(3, map.size());
        assertTrue(map.get("foo") instanceof JsonNode);
        assertTrue((Boolean)map.get("isBarValue"));
        assertEquals(new Double(8), map.get("bar"));
    }

    @Test
    public void testAnnotatedMethod3() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent component = new AnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        //Since AnnotatedComponent2 has two annotated methods we need to set the method to call
        context.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "doStuff3", PropertyScope.INVOCATION);
        InvocationResult result = resolver.invoke(component, context);
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
        assertTrue(result.getResult() instanceof Map);
        Map<?, ?> map = (Map<?, ?>)result.getResult();
        assertEquals(2, map.size());
        assertTrue(map.get("foo") instanceof JsonNode);
        assertTrue(map.get("bar") instanceof List);
        assertEquals(2, ((List)map.get("bar")).size());
    }

    @Test
    public void testAnnotatedMethodRequiredMissing() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent component = new AnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        //Since AnnotatedComponent2 has two annotated methods we need to set the method to call
        context.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "doStuff4", PropertyScope.INVOCATION);
        try
        {
            resolver.invoke(component, context);
            fail("The xpath expression returned null, nbut a value was required");
        }
        catch (RequiredValueException e)
        {
            //expected
        }
    }

    @Test
    public void testAnnotatedMethodMissingNotRequired() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent component = new AnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        //Since AnnotatedComponent2 has two annotated methods we need to set the method to call
        context.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "doStuff5", PropertyScope.INVOCATION);
        InvocationResult result = resolver.invoke(component, context);
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
        assertTrue(result.getResult() instanceof Map);
        Map<?, ?> map = (Map<?, ?>)result.getResult();
        assertEquals(1, map.size());
        assertNull(map.get("foo"));
    }

    @Test
    public void testIllegalAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        IllegalAnnotatedComponent component = new IllegalAnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        try
        {
            resolver.invoke(component, context);
            fail("Annotated parameter has an illegal return type argument");
        }
        catch (TransformerException e)
        {
            //expected
        }
    }
}
