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
import org.mule.impl.model.resolvers.ArrayEntryPointResolver;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.model.InvocationResult;

public class ArrayEntryPointResolverTestCase extends AbstractMuleTestCase
{

    public void testArrayMatch() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
        InvocationResult ctx = resolver.invoke(new FruitBowl(), getTestEventContext(new Fruit[]{new Apple(), new Orange()}));
        assertEquals(ctx.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);

    }

    public void testArrayMatchGenericFail() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
        InvocationResult ctx = resolver.invoke(new FruitBowl(), getTestEventContext(new Object[]{new Apple(), new Orange()}));
        assertEquals(ctx.getState(), InvocationResult.STATE_INVOKED_FAILED);
    }


    public void testArrayMatchFail() throws Exception
    {
        AbstractArgumentEntryPointResolver resolver = new ArrayEntryPointResolver();
        InvocationResult ctx = resolver.invoke(new Apple(), getTestEventContext(new Object[]{"blah"}));
        assertEquals(ctx.getState(), InvocationResult.STATE_INVOKED_FAILED);
    }
}
