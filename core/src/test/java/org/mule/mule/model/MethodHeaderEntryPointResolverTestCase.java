/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mule.model;

import org.mule.api.MuleEventContext;
import org.mule.api.model.InvocationResult;
import org.mule.api.transport.PropertyScope;
import org.mule.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.transport.NullPayload;

public class MethodHeaderEntryPointResolverTestCase extends AbstractMuleTestCase
{
    private MethodHeaderPropertyEntryPointResolver resolver;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        resolver = new MethodHeaderPropertyEntryPointResolver();
    }

    public void testMethodSetPass() throws Exception
    {
        MuleEventContext ctx = getTestEventContext("blah");
        ctx.getMessage().setProperty("method", "someBusinessMethod", PropertyScope.INBOUND);
        
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
        assertInvocationWasSuccessful(result);
    }

    public void testMethodSetWithNoArgsPass() throws Exception
    {
        MuleEventContext ctx = getTestEventContext(NullPayload.getInstance());
        ctx.getMessage().setProperty("method", "wash", PropertyScope.INBOUND);
        
        InvocationResult result = resolver.invoke(new Apple(), ctx);
        assertInvocationWasSuccessful(result);
        assertEquals("wash", result.getMethodCalled());
    }

    public void testCustomMethodProperty() throws Exception
    {
        resolver.setMethodProperty("serviceMethod");
        
        MuleEventContext ctx = getTestEventContext("blah");
        ctx.getMessage().setProperty("serviceMethod", "someBusinessMethod", PropertyScope.INBOUND);
        
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
        assertInvocationWasSuccessful(result);
    }

    public void testCustomMethodPropertyFail() throws Exception
    {
        resolver.setMethodProperty("serviceMethod");
        
        MuleEventContext ctx = getTestEventContext("blah");
        ctx.getMessage().setProperty("serviceMethod", "noMethod", PropertyScope.INBOUND);
        
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
        assertInvocationFailed(result);
    }
    
    public void testMethodPropertyFail() throws Exception
    {
        resolver.setMethodProperty("serviceMethod");
        
        MuleEventContext ctx = getTestEventContext("blah");
        ctx.getMessage().setProperty("myMethod", "someBusinessMethod", PropertyScope.INBOUND);
        
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
        assertInvocationFailed(result);
    }

    public void testMethodPropertyMismatch() throws Exception
    {
        MuleEventContext ctx = getTestEventContext("blah");
        ctx.getMessage().setProperty("method", "noMethod", PropertyScope.INBOUND);
        
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), ctx);
        assertInvocationFailed(result);
    }

    /**
     * If a method with correct name is available then it should be used if the
     * parameter type is assignable from the payload type and not just if there is an
     * exact match. See MULE-3636.
     */
    public void testMethodPropertyParameterAssignableFromPayload() throws Exception
    {
        MuleEventContext ctx = getTestEventContext(new Apple());
        ctx.getMessage().setProperty("method", "wash", PropertyScope.INBOUND);

        InvocationResult result = resolver.invoke(new TestFruitCleaner(), ctx);
        assertInvocationWasSuccessful(result);
    }
    
    private void assertInvocationWasSuccessful(InvocationResult result)
    {
        assertEquals(InvocationResult.STATE_INVOKED_SUCESSFUL, result.getState());
    }

    private void assertInvocationFailed(InvocationResult result)
    {
        assertEquals(InvocationResult.STATE_INVOKED_FAILED, result.getState());
    }

    public static class TestFruitCleaner
    {
        public void wash(Fruit fruit)
        {
            // dummy
        }

        public void polish(Fruit fruit)
        {
            // dummy
        }
    }
}
