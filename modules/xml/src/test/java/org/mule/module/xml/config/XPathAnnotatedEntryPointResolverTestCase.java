/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.model.InvocationResult;
import org.mule.api.transport.PropertyScope;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Map;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XPathAnnotatedEntryPointResolverTestCase extends AbstractMuleContextTestCase
{

    public static final String TEST_PAYLOAD = "<foo><bar>4</bar><bar>8</bar></foo>";

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
        Map<?, ?> map = (Map<?, ?>) result.getResult();
        assertEquals(3, map.size());
        assertTrue(map.get("foo") instanceof Element);
        assertTrue((Boolean) map.get("isBarValue"));
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
        Map<?, ?> map = (Map<?, ?>) result.getResult();
        assertEquals(3, map.size());
        assertTrue(map.get("foo") instanceof Document);
        assertTrue((Boolean) map.get("isBarValue"));
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
        Map<?, ?> map = (Map<?, ?>) result.getResult();
        assertEquals(2, map.size());
        assertTrue(map.get("foo") instanceof Node);
        assertTrue(map.get("bar") instanceof NodeList);
        assertEquals(2, ((NodeList) map.get("bar")).getLength());
    }

    @Test(expected = RequiredValueException.class)
    public void testAnnotatedMethodRequiredMissing() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent component = new AnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        //Since AnnotatedComponent2 has two annotated methods we need to set the method to call
        context.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "doStuff4", PropertyScope.INVOCATION);

        resolver.invoke(component, context);
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
        Map<?, ?> map = (Map<?, ?>) result.getResult();
        assertEquals(1, map.size());
        assertNull(map.get("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        IllegalAnnotatedComponent component = new IllegalAnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);

        resolver.invoke(component, context);
    }
}
