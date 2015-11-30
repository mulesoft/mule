/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.mule.model;

import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.model.resolvers.ArrayEntryPointResolver;
import org.mule.model.resolvers.EntryPointNotFoundException;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.fruit.FruitLover;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacyEntryPointResolverTestCase extends AbstractMuleContextTestCase
{

    /** Name of the method override property on the event. */
    private static final String METHOD_PROPERTY_NAME = MuleProperties.MULE_METHOD_PROPERTY;

    /** Name of the non-existent method. */
    private static final String INVALID_METHOD_NAME = "nosuchmethod";

    @Test
    public void testExplicitMethodMatch() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.invoke(new WaterMelon(), getTestEventContext("blah"));
        }
        catch (MuleException e)
        {
            fail("Test should have passed: " + e);
        }
    }

    @Test
    public void testExplicitMethodMatchComplexObject() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.invoke(new FruitBowl(), getTestEventContext(new FruitLover("Mmmm")));
        }
        catch (MuleException e)
        {
            fail("Test should have passed: " + e);
        }
    }

    @Test
    public void testExplicitMethodMatchSetArrayFail() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.invoke(new FruitBowl(), getTestEventContext(new Fruit[]{new Apple(), new Orange()}));
            fail("Test should have failed because the arguments were not wrapped properly: ");
        }
        catch (MuleException e)
        {
            //expected
        }
    }

    @Test
    public void testExplicitMethodMatchSetArrayPass() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.invoke(new FruitBowl(), getTestEventContext(new Object[]{new Fruit[]{new Apple(), new Orange()}}));
        }
        catch (MuleException e)
        {
            fail("Test should have passed: " + e);
        }
    }

    /* this tests the same as above except it uses the {@link ArrayEntryPointResolver} and does not wrap the args with an array
     */
    @Test
    public void testExplicitMethodMatchSetArrayPassUsingExplicitResolver() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.addEntryPointResolver(new ArrayEntryPointResolver());
            resolver.invoke(new FruitBowl(), getTestEventContext(new Fruit[]{new Apple(), new Orange()}));
        }
        catch (MuleException e)
        {
            fail("Test should have passed: " + e);
        }
    }

//    public ComponentMethodMapping[] getComponentMappings()
//    {
//        ComponentMethodMapping[] mappings = new ComponentMethodMapping[5];
//        mappings[0] = new ComponentMethodMapping(WaterMelon.class, "myEventHandler", MuleEvent.class);
//        mappings[1] = new ComponentMethodMapping(FruitBowl.class, "consumeFruit", FruitLover.class);
//        // see testArrayArgumentResolution
//        mappings[2] = new ComponentMethodMapping(FruitBowl.class, "setFruit", Fruit[].class);
//        // see testListArgumentResolution
//        mappings[3] = new ComponentMethodMapping(FruitBowl.class, "setFruit", Collection.class);
//        mappings[4] = new ComponentMethodMapping(Banana.class, "peelEvent", EventObject.class);
//
//        // TODO This fails because "implementation" can no longer be a container reference, it must be
//        // an actual object and InvocationHandler does not have a default constructor.
//
//        // test proxy objects
//        //mappings[5] = new ComponentMethodMapping(InvocationHandler.class, "invoke", FruitLover.class, true);
//
//        return mappings;
//    }

//    @Override
//    public UMODescriptor getDescriptorToResolve(String className) throws Exception
//    {
//        UMODescriptor descriptor = super.getDescriptorToResolve(className);
//        descriptor.setInboundRouter(new DefaultInboundRouterCollection());
//        Endpoint endpoint = new MuleEndpoint("test://foo", true);
//
//        if (className.equals(FruitBowl.class.getName()))
//        {
//            endpoint.setTransformers(CollectionUtils.singletonList(new ObjectToFruitLover()));
//            descriptor.getInboundRouter().addEndpoint(endpoint);
//        }
//        else if (className.equals(InvocationHandler.class.getName()))
//        {
//            endpoint.setTransformers(CollectionUtils.singletonList(new ObjectToFruitLover()));
//            descriptor.getInboundRouter().addEndpoint(endpoint);
//        }
//        return descriptor;
//    }

    /**
     * Tests entrypoint discovery when there is more than one discoverable method
     * with MuleEventContext parameter.
     */
    @Test
    public void testFailEntryPointMultiplePayloadMatches() throws Exception
    {
        EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

        try
        {
            RequestContext.setEvent(getTestEvent("Hello"));
            resolverSet.invoke(new MultiplePayloadsTestObject(), RequestContext.getEventContext());
            fail("Should have failed to find entrypoint.");
        }
        catch (EntryPointNotFoundException itex)
        {
            // expected
        }
    }

    /**
     * If there was a method parameter specified to override the discovery mechanism
     * and no such method exists, an exception should be thrown, and no fallback to
     * the default discovery should take place.
     */
    @Test
    public void testMethodOverrideDoesNotFallback() throws Exception
    {
        EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();
        RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

        // those are usually set on the endpoint and copied over to the message
        final String methodName = "nosuchmethod";
        final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
        RequestContext.getEventContext().getMessage().setOutboundProperty(propertyName, methodName);

        resolverSet.invoke(new FruitBowl(), RequestContext.getEventContext());
        // fail("Should have failed to find an entrypoint.");
    }

    /**
     * If there was a method parameter specified to override the discovery mechanism
     * and a Callable instance is serving the request, call the Callable, ignore the
     * method override parameter.
     */
    @Test
    public void testMethodOverrideIgnoredWithCallable() throws Exception
    {
        EntryPointResolverSet resolver = new LegacyEntryPointResolverSet();

        RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

        // those are usually set on the endpoint and copied over to the message
        RequestContext.getEventContext().getMessage().setOutboundProperty(METHOD_PROPERTY_NAME,
                                                                          INVALID_METHOD_NAME);

        Apple apple = new Apple();
        apple.setAppleCleaner(new FruitCleaner()
        {
            public void wash(Fruit fruit)
            {
                // dummy
            }

            public void polish(Fruit fruit)
            {
                // dummy
            }
        });
        resolver.invoke(apple, RequestContext.getEventContext());
    }

    /**
     * If there was a method parameter specified to override the discovery mechanism
     * and a target instance has a method accepting MuleEventContext, proceed to call
     * this method, ignore the method override parameter.
     */
    @Test
    public void testMethodOverrideIgnoredWithEventContext() throws Exception
    {
        EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

        RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

        // those are usually set on the endpoint and copied over to the message
        final String methodName = "nosuchmethod";
        final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
        RequestContext.getEventContext().getMessage().setOutboundProperty(propertyName, methodName);

        try
        {
            resolverSet.invoke(new Kiwi(), RequestContext.getEventContext());
            fail("no such method on service");
        }
        catch (EntryPointNotFoundException e)
        {
            // expected
        }
    }

    /** Test for proper resolution of a method that takes an array as argument. */
    // TODO MULE-1088: currently fails, therefore disabled
    @Test
    public void testArrayArgumentResolution() throws Exception
    {
        EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

        Object payload = new Object[]{new Fruit[]{new Apple(), new Banana()}};
        RequestContext.setEvent(getTestEvent(payload));

        FruitBowl bowl = new FruitBowl();
        assertFalse(bowl.hasApple());
        assertFalse(bowl.hasBanana());

        resolverSet.invoke(bowl, RequestContext.getEventContext());

        assertTrue(bowl.hasApple());
        assertTrue(bowl.hasBanana());
    }

    /** Test for proper resolution of a method that takes a List as argument. */
    @Test
    public void testListArgumentResolution() throws Exception
    {
        EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();
        Object payload = Arrays.asList(new Fruit[]{new Apple(), new Banana()});
        RequestContext.setEvent(getTestEvent(payload));

        FruitBowl bowl = new FruitBowl();
        assertFalse(bowl.hasApple());
        assertFalse(bowl.hasBanana());

        resolverSet.invoke(bowl, RequestContext.getEventContext());

        assertTrue(bowl.hasApple());
        assertTrue(bowl.hasBanana());
    }

    /** Test for proper resolution of an existing method specified as override */
    @Test
    public void testExplicitOverride() throws Exception
    {
        EntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

        Object payload = Arrays.asList(new Fruit[]{new Apple(), new Banana()});
        RequestContext.setEvent(getTestEvent(payload));

        final String methodName = "setFruit";
        final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
        RequestContext.getEventContext().getMessage().setOutboundProperty(propertyName, methodName);

        FruitBowl bowl = new FruitBowl();
        assertFalse(bowl.hasApple());
        assertFalse(bowl.hasBanana());

        resolverSet.invoke(bowl, RequestContext.getEventContext());

        assertTrue(bowl.hasApple());
        assertTrue(bowl.hasBanana());
    }
}
