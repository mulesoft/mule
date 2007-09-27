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

import org.mule.config.MuleProperties;
import org.mule.impl.RequestContext;
import org.mule.impl.model.resolvers.ArrayEntryPointResolver;
import org.mule.impl.model.resolvers.EntryPointNotFoundException;
import org.mule.impl.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.fruit.FruitLover;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOEntryPointResolverSet;

import java.util.Arrays;

public class LegacyEntryPointResolverTestCase extends AbstractMuleTestCase
{
    /** Name of the method override property on the event. */
    private static final String METHOD_PROPERTY_NAME = MuleProperties.MULE_METHOD_PROPERTY;

    /** Name of the non-existent method. */
    private static final String INVALID_METHOD_NAME = "nosuchmethod";

    public void testExplicitMethodMatch() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.invoke(new WaterMelon(), getTestEventContext("blah"));
        }
        catch (UMOException e)
        {
            fail("Test should have passed: " + e);
        }
    }

    public void testExplicitMethodMatchComplexObject() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.invoke(new FruitBowl(), getTestEventContext(new FruitLover("Mmmm")));
        }
        catch (UMOException e)
        {
            fail("Test should have passed: " + e);
        }
    }


    public void testExplicitMethodMatchSetArrayFail() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.invoke(new FruitBowl(), getTestEventContext(new Fruit[]{new Apple(), new Orange()}));
            fail("Test should have failed because the arguments were not wrapped properly: ");

        }
        catch (UMOException e)
        {
            //expected
        }
    }

    public void testExplicitMethodMatchSetArrayPass() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.invoke(new FruitBowl(), getTestEventContext(new Object[]{new Fruit[]{new Apple(), new Orange()}}));
        }
        catch (UMOException e)
        {
            fail("Test should have passed: " + e);
        }
    }

    /* this tests the same as above except it uses the {@link ArrayEntryPointResolver} and does not wrap the args with an array
     */
    public void testExplicitMethodMatchSetArrayPassUsingExplicitResolver() throws Exception
    {
        try
        {
            LegacyEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();
            resolver.addEntryPointResolver(new ArrayEntryPointResolver());
            resolver.invoke(new FruitBowl(), getTestEventContext(new Fruit[]{new Apple(), new Orange()}));
        }
        catch (UMOException e)
        {
            fail("Test should have passed: " + e);
        }
    }

//    public ComponentMethodMapping[] getComponentMappings()
//    {
//        ComponentMethodMapping[] mappings = new ComponentMethodMapping[5];
//        mappings[0] = new ComponentMethodMapping(WaterMelon.class, "myEventHandler", UMOEvent.class);
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

//    // @Override
//    public UMODescriptor getDescriptorToResolve(String className) throws Exception
//    {
//        UMODescriptor descriptor = super.getDescriptorToResolve(className);
//        descriptor.setInboundRouter(new InboundRouterCollection());
//        UMOEndpoint endpoint = new MuleEndpoint("test://foo", true);
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
     * with UMOEventContext parameter.
     */
    public void testFailEntryPointMultiplePayloadMatches() throws Exception
    {
        UMOEntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

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
    public void testMethodOverrideDoesNotFallback() throws Exception
    {
        UMOEntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();
        RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

        // those are usually set on the endpoint and copied over to the message
        final String methodName = "nosuchmethod";
        final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
        RequestContext.getEventContext().getMessage().setProperty(propertyName, methodName);

        resolverSet.invoke(new FruitBowl(), RequestContext.getEventContext());
        // fail("Should have failed to find an entrypoint.");
    }

    /**
     * If there was a method parameter specified to override the discovery mechanism
     * and a Callable instance is serving the request, call the Callable, ignore the
     * method override parameter.
     */
    public void testMethodOverrideIgnoredWithCallable() throws Exception
    {
        UMOEntryPointResolverSet resolver = new LegacyEntryPointResolverSet();

        RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

        // those are usually set on the endpoint and copied over to the message
        RequestContext.getEventContext().getMessage().setProperty(METHOD_PROPERTY_NAME,
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
     * and a target instance has a method accepting UMOEventContext, proceed to call
     * this method, ignore the method override parameter.
     */
    public void testMethodOverrideIgnoredWithEventContext() throws Exception
    {
        UMOEntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

        RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

        // those are usually set on the endpoint and copied over to the message
        final String methodName = "nosuchmethod";
        final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
        RequestContext.getEventContext().getMessage().setProperty(propertyName, methodName);

        try
        {
            resolverSet.invoke(new Kiwi(), RequestContext.getEventContext());
            fail("no such method on component");
        }
        catch (EntryPointNotFoundException e)
        {
            // expected
        }
    }

    /** Test for proper resolution of a method that takes an array as argument. */
    // TODO MULE-1088: currently fails, therefore disabled
    public void testArrayArgumentResolution() throws Exception
    {
        UMOEntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

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
    public void testListArgumentResolution() throws Exception
    {
        UMOEntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();
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
    public void testExplicitOverride() throws Exception
    {
        UMOEntryPointResolverSet resolverSet = new LegacyEntryPointResolverSet();

        Object payload = Arrays.asList(new Fruit[]{new Apple(), new Banana()});
        RequestContext.setEvent(getTestEvent(payload));

        final String methodName = "setFruit";
        final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
        RequestContext.getEventContext().getMessage().setProperty(propertyName, methodName);

        FruitBowl bowl = new FruitBowl();
        assertFalse(bowl.hasApple());
        assertFalse(bowl.hasBanana());

        resolverSet.invoke(bowl, RequestContext.getEventContext());

        assertTrue(bowl.hasApple());
        assertTrue(bowl.hasBanana());
    }
}
