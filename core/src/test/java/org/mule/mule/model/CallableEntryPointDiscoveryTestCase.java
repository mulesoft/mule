/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mule.model;

import org.mule.impl.model.resolvers.CallableEntryPointResolver;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.umo.model.InvocationResult;

public class CallableEntryPointDiscoveryTestCase extends AbstractMuleTestCase
{

    public void testBadMatch() throws Exception
    {
        CallableEntryPointResolver resolver = new CallableEntryPointResolver();
        InvocationResult result = resolver.invoke(new WaterMelon(), getTestEventContext(new StringBuffer("foo")));
        assertEquals("Component doesn't implement Callable", result.getState(), InvocationResult.STATE_INVOKE_NOT_SUPPORTED);
    }

    public void testGoodMatch() throws Exception
    {
        CallableEntryPointResolver resolver = new CallableEntryPointResolver();
        InvocationResult result = resolver.invoke(new Apple(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
    }
}
