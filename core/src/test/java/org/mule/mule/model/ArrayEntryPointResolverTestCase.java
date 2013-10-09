/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.mule.model;

import org.mule.api.model.InvocationResult;
import org.mule.model.resolvers.AbstractArgumentEntryPointResolver;
import org.mule.model.resolvers.ArrayEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArrayEntryPointResolverTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testArrayMatch() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
        InvocationResult ctx = resolver.invoke(new FruitBowl(), getTestEventContext(new Fruit[]{new Apple(), new Orange()}));
        assertEquals(ctx.getState(), InvocationResult.State.SUCCESSFUL);

    }

    @Test
    public void testArrayMatchGenericFail() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
        InvocationResult ctx = resolver.invoke(new FruitBowl(), getTestEventContext(new Object[]{new Apple(), new Orange()}));
        assertEquals(ctx.getState(), InvocationResult.State.FAILED);
    }


    @Test
    public void testArrayMatchFail() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
        InvocationResult ctx = resolver.invoke(new Apple(), getTestEventContext(new Object[]{"blah"}));
        assertEquals(ctx.getState(), InvocationResult.State.FAILED);
    }
}
