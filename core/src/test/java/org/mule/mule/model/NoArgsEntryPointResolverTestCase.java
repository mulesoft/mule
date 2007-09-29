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

import org.mule.impl.model.resolvers.AbstractArgumentEntryPointResolver;
import org.mule.impl.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.providers.NullPayload;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.InvalidSatsuma;
import org.mule.umo.model.InvocationResult;

public class NoArgsEntryPointResolverTestCase extends AbstractMuleTestCase
{
    public void testExplicitMethodMatch() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        resolver.addMethod("bite");
        InvocationResult result = resolver.invoke(new InvalidSatsuma(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
    }

    public void testExplicitMethodMatch2() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        resolver.addMethod("wash");
        InvocationResult result = resolver.invoke(new Apple(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
    }

    public void testDynamicMethodMatchFail() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        InvocationResult result = resolver.invoke(new Apple(), getTestEventContext("blah"));
        assertEquals("Apple component has a number of matching method, so should have failed",
                result.getState(), InvocationResult.STATE_INVOKED_FAILED);
    }

    public void testDynamicMethodMatchPass() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        InvocationResult result = resolver.invoke(new InvalidSatsuma(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
    }

    public void testDynamicMethodMatchFailOnWildcardMatch() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        assertTrue(resolver.removeIgnorredMethod("is*"));
        InvocationResult result = resolver.invoke(new InvalidSatsuma(), getTestEventContext("blah"));
        assertEquals("Satsuma component has a number of matching method, so should have failed",
                result.getState(), InvocationResult.STATE_INVOKED_FAILED);
    }

    /** Having a null payload should make no difference */
    public void testExplicitMethodMatchAndNullPayload() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        resolver.addMethod("wash");
        InvocationResult result = resolver.invoke(new Apple(), getTestEventContext(NullPayload.getInstance()));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
    }
}
