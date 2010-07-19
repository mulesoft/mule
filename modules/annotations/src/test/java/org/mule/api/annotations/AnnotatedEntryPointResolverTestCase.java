/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations;

import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.model.InvocationResult;
import org.mule.api.transport.PropertyScope;
import org.mule.component.simple.EchoComponent;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.tck.AbstractMuleTestCase;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

public class AnnotatedEntryPointResolverTestCase extends AbstractMuleTestCase
{
    public static final String TEST_PAYLOAD = "<foo><bar>Hello</bar></foo>";

    public void testAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent2 component = new AnnotatedComponent2();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        context.getMessage().setProperty("name", "Ross", PropertyScope.OUTBOUND);
        //Since AnnotatedComponent2 has two annotated methods we need to set the method to call
        context.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "doSomething", PropertyScope.INVOCATION);
        InvocationResult result = resolver.invoke(component, context);
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
        //We need to parse the xml to normalise it so that the final assert passes
        Document doc = DocumentHelper.parseText(TEST_PAYLOAD);
        assertEquals("Hello:Ross:" + doc.asXML(), result.getResult());
    }

    public void testDefaultAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent1 component = new AnnotatedComponent1();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        context.getMessage().setProperty("name", "Ross", PropertyScope.OUTBOUND);
        //No need to set the method property if the component only has a single annotated method
        InvocationResult result = resolver.invoke(component, context);
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
        //We need to parse the xml to normalise it so that the final assert passes
        Document doc = DocumentHelper.parseText(TEST_PAYLOAD);
        assertEquals("Hello:Ross:" + doc.asXML(), result.getResult());
    }

    public void testAnnotatedMethodWithoutMethodHeader() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent2 component = new AnnotatedComponent2();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        InvocationResult result = resolver.invoke(component, context);
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_FAILED);
    }

    public void testNonAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        InvocationResult result = resolver.invoke(new EchoComponent(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKE_NOT_SUPPORTED);
    }

    //Test that we don't toucvh any non-Mule evaluator annotations
    public void testNonMuleAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        MuleEventContext event = getTestEventContext(new HashMap());
        event.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "nonExpressionAnnotation", PropertyScope.INVOCATION);
        InvocationResult result = resolver.invoke(new AnnotatedComponent2(), event);
        assertEquals(result.getState(), InvocationResult.STATE_INVOKE_NOT_SUPPORTED);
    }

    public void testAnnotatedMethodOnProxyWithMethodSet() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();

        Enhancer e = new Enhancer();
        e.setSuperclass(AnnotatedComponent2.class);
        e.setCallback(new DummyMethodCallback());
        Object proxy = e.create();

        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        context.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "doSomething", PropertyScope.INVOCATION);
        InvocationResult result = resolver.invoke(proxy, context);
        assertEquals(result.getState(), InvocationResult.STATE_INVOKE_NOT_SUPPORTED);
    }

    private class DummyMethodCallback implements MethodInterceptor
    {
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable
        {
            System.out.println("before: " + method.getName());
            Object r = proxy.invokeSuper(obj, args);
            System.out.println("after: " + method.getName());

            //Add handler code here
            return r;
        }
    }
    
    private class DummyComponentProxyHandler implements InvocationHandler
    {
        private Object component;

        private DummyComponentProxyHandler(Object component)
        {
            this.component = component;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            return method.invoke(component, args);
        }
    }
}

