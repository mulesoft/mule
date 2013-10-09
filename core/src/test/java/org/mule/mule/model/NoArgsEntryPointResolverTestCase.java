/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.mule.model;

import org.mule.api.model.InvocationResult;
import org.mule.model.resolvers.AbstractArgumentEntryPointResolver;
import org.mule.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.InvalidSatsuma;
import org.mule.transport.NullPayload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NoArgsEntryPointResolverTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testExplicitMethodMatch() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        resolver.addMethod("bite");
        InvocationResult result = resolver.invoke(new InvalidSatsuma(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    }

    @Test
    public void testExplicitMethodMatch2() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        resolver.addMethod("wash");
        InvocationResult result = resolver.invoke(new Apple(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    }

    @Test
    public void testDynamicMethodMatchFail() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        InvocationResult result = resolver.invoke(new Apple(), getTestEventContext("blah"));
        assertEquals("Apple service has a number of matching method, so should have failed",
                result.getState(), InvocationResult.State.FAILED);
    }

    @Test
    public void testDynamicMethodMatchPass() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        InvocationResult result = resolver.invoke(new InvalidSatsuma(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    }

    @Test
    public void testDynamicMethodMatchFailOnWildcardMatch() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        assertTrue(resolver.removeIgnoredMethod("is*"));
        InvocationResult result = resolver.invoke(new InvalidSatsuma(), getTestEventContext("blah"));
        assertEquals("Satsuma service has a number of matching method, so should have failed",
                result.getState(), InvocationResult.State.FAILED);
    }

    /** Having a null payload should make no difference */
    @Test
    public void testExplicitMethodMatchAndNullPayload() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new NoArgumentsEntryPointResolver();
        resolver.addMethod("wash");
        InvocationResult result = resolver.invoke(new Apple(), getTestEventContext(NullPayload.getInstance()));
        assertEquals(result.getState(), InvocationResult.State.SUCCESSFUL);
    }
}
