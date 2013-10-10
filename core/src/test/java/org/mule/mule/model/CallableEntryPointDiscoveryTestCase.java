/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.mule.model;

import org.mule.api.model.InvocationResult;
import org.mule.model.resolvers.CallableEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.WaterMelon;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CallableEntryPointDiscoveryTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testBadMatch() throws Exception
    {
        CallableEntryPointResolver resolver = new CallableEntryPointResolver();
        InvocationResult result = resolver.invoke(new WaterMelon(), getTestEventContext(new StringBuffer("foo")));
        assertEquals("Service doesn't implement Callable", result.getState(), InvocationResult.State.NOT_SUPPORTED);
    }

    @Test
    public void testGoodMatch() throws Exception
    {
        CallableEntryPointResolver resolver = new CallableEntryPointResolver();
        InvocationResult result = resolver.invoke(new Apple(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    }
}
