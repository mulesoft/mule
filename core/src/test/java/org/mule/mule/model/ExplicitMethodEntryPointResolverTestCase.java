/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.mule.model;

import org.mule.api.MuleEventContext;
import org.mule.api.model.InvocationResult;
import org.mule.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExplicitMethodEntryPointResolverTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testMethodSetPass() throws Exception
    {
        ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
        resolver.addMethod("someBusinessMethod");
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    }

    @Test
    public void testMethodSetMatchFirst() throws Exception
    {
        ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
        resolver.addMethod("someBusinessMethod");
        resolver.addMethod("someSetter");
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    }

    @Test
    public void testMethodNotFound() throws Exception
    {
        ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
        resolver.addMethod("noMethod");
        resolver.addMethod("noMethod2");
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.State.FAILED);
    }

    @Test
    public void testNoMethodSet() throws Exception
    {
        ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
        try
        {
            resolver.invoke(new MultiplePayloadsTestObject(), getTestEventContext("blah"));
            fail("method property is not set, this should cause an error");
        }
        catch (IllegalStateException e)
        {
            //Expected
        }
    }

    /**
     * If a method with correct name is available then it should be used is the
     * parameter type is assignable from the payload type and not just if there is an
     * exact match. See MULE-3636.
     *
     * @throws Exception
     */
    @Test
    public void testMethodPropertyParameterAssignableFromPayload() throws Exception
    {
        ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
        resolver.addMethod("wash");
        MuleEventContext ctx = getTestEventContext(new Apple());
        InvocationResult result = resolver.invoke(new TestFruitCleaner(), ctx);
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    }

    /**
     * If a method with correct name is available then it should be used even if one
     * or more parameter types in the payload are null, as long as the parameter
     * count matches.
     *
     * @throws Exception
     */
    @Test
    public void testMethodPropertyParameterNull() throws Exception
    {
        ExplicitMethodEntryPointResolver resolver = new ExplicitMethodEntryPointResolver();
        resolver.addMethod("someOtherBusinessMethod");
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(),
            getTestEventContext(new Object[]{null, "blah"}));
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
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
