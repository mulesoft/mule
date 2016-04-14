/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
        InvocationResult result = resolver.invoke(new WaterMelon(), getTestEventContext(new StringBuilder("foo")));
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
