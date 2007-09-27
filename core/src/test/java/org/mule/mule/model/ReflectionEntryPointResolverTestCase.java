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

import org.mule.impl.RequestContext;
import org.mule.impl.model.resolvers.ReflectionEntryPointResolver;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitLover;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.umo.model.InvocationResult;

public class ReflectionEntryPointResolverTestCase extends AbstractMuleTestCase
{

    public void testExplicitMethodMatch() throws Exception
    {
        ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
        InvocationResult result = resolver.invoke(new WaterMelon(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
    }

    public void testExplicitMethodMatchComplexObject() throws Exception
    {
        ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
        InvocationResult result = resolver.invoke(new FruitBowl(), getTestEventContext(new FruitLover("Mmmm")));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
    }


    public void testExplicitMethodMatchSetArrayFail() throws Exception
    {
        ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
        InvocationResult result = resolver.invoke(new FruitBowl(), getTestEventContext(new Fruit[]{new Apple(), new Orange()}));
        assertEquals("Test should have failed because the arguments were not wrapped properly: ",
                result.getState(), InvocationResult.STATE_INVOKED_FAILED);
    }

    public void testExplicitMethodMatchSetArrayPass() throws Exception
    {
        ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
        InvocationResult result = resolver.invoke(new FruitBowl(), getTestEventContext(new Object[]{new Fruit[]{new Apple(), new Orange()}}));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
    }

    /**
     * Tests entrypoint discovery when there is more than one discoverable method
     * with UMOEventContext parameter.
     */
    public void testFailEntryPointMultiplePayloadMatches() throws Exception
    {
        ReflectionEntryPointResolver resolver = new ReflectionEntryPointResolver();
        RequestContext.setEvent(getTestEvent("Hello"));
        InvocationResult result = resolver.invoke(new MultiplePayloadsTestObject(), RequestContext.getEventContext());
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_FAILED);
    }
}
