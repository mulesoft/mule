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
import org.mule.impl.TooManySatisfiableMethodsException;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.model.resolvers.DynamicEntryPointResolver;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.tck.model.AbstractEntryPointDiscoveryTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.fruit.FruitLover;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.tck.testmodels.fruit.ObjectToFruitLover;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.model.UMOEntryPointResolver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;

public class DynamicEntryPointDiscoveryTestCase extends AbstractEntryPointDiscoveryTestCase
{
    /**
     * Name of the method override property on the event.
     */
    private static final String METHOD_PROPERTY_NAME = MuleProperties.MULE_METHOD_PROPERTY;

    /**
     * Name of the non-existent method.
     */
    private static final String INVALID_METHOD_NAME = "nosuchmethod";

    public ComponentMethodMapping[] getComponentMappings()
    {
        ComponentMethodMapping[] mappings = new ComponentMethodMapping[5];
        mappings[0] = new ComponentMethodMapping(WaterMelon.class, "myEventHandler", UMOEvent.class);
        mappings[1] = new ComponentMethodMapping(FruitBowl.class, "consumeFruit", FruitLover.class);
        // see testArrayArgumentResolution
        mappings[2] = new ComponentMethodMapping(FruitBowl.class, "setFruit", Fruit[].class);
        // see testListArgumentResolution
        mappings[3] = new ComponentMethodMapping(FruitBowl.class, "setFruit", Collection.class);
        mappings[4] = new ComponentMethodMapping(Banana.class, "peelEvent", EventObject.class);

        // TODO This fails because "implementation" can no longer be a container reference, it must be
        // an actual object and InvocationHandler does not have a default constructor.
        
        // test proxy objects
        //mappings[5] = new ComponentMethodMapping(InvocationHandler.class, "invoke", FruitLover.class, true);
        
        return mappings;
    }

    // @Override
    public UMODescriptor getDescriptorToResolve(String className) throws Exception
    {
        UMODescriptor descriptor = super.getDescriptorToResolve(className);
        descriptor.setInboundRouter(new InboundRouterCollection());
        UMOEndpoint endpoint = new MuleEndpoint("test://foo", true);

        if (className.equals(FruitBowl.class.getName()))
        {
            endpoint.setTransformer(new ObjectToFruitLover());
            descriptor.getInboundRouter().addEndpoint(endpoint);
        }
        else if (className.equals(InvocationHandler.class.getName()))
        {
            endpoint.setTransformer(new ObjectToFruitLover());
            descriptor.getInboundRouter().addEndpoint(endpoint);
        }
        return descriptor;
    }

    public UMOEntryPointResolver getEntryPointResolver()
    {
        return new DynamicEntryPointResolver();
    }

    /**
     * Tests entrypoint discovery when there is more than one discoverable method
     * with UMOEventContext parameter.
     */
    public void testFailEntryPointMultipleEventContextMatches() throws Exception
    {
        UMOEntryPointResolver epd = getEntryPointResolver();
        UMODescriptor descriptor = getTestDescriptor("badContexts", MultipleEventContextsTestObject.class
            .getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try
        {
            RequestContext.setEvent(getTestEvent("Hello"));
            ep.invoke(new MultipleEventContextsTestObject(), RequestContext.getEventContext());
            fail("Should have failed to find entrypoint.");
        }
        catch (InvocationTargetException itex)
        {
            final Throwable cause = itex.getCause();
            if (cause instanceof TooManySatisfiableMethodsException)
            {
                // expected
            }
            else
            {
                throw itex;
            }
        }
        finally
        {
            RequestContext.setEvent(null);
        }

    }

    /**
     * Tests entrypoint discovery when there is more than one discoverable method
     * with UMOEventContext parameter.
     */
    public void testFailEntryPointMultiplePayloadMatches() throws Exception
    {
        UMOEntryPointResolver epd = getEntryPointResolver();
        UMODescriptor descriptor = getTestDescriptor("badPayloads", MultiplePayloadsTestObject.class
            .getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try
        {
            RequestContext.setEvent(getTestEvent("Hello"));
            ep.invoke(new MultiplePayloadsTestObject(), RequestContext.getEventContext());
            fail("Should have failed to find entrypoint.");
        }
        catch (TooManySatisfiableMethodsException itex)
        {
            // expected
        }
        finally
        {
            RequestContext.setEvent(null);
        }
    }

    /**
     * If there was a method parameter specified to override the discovery mechanism
     * and no such method exists, an exception should be thrown, and no fallback to
     * the default discovery should take place.
     */
    public void testMethodOverrideDoesNotFallback() throws Exception
    {
        UMOEntryPointResolver epd = getEntryPointResolver();
        UMODescriptor descriptor = getDescriptorToResolve(FruitBowl.class.getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try
        {
            RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

            // those are usually set on the endpoint and copied over to the message
            final String methodName = "nosuchmethod";
            final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
            RequestContext.getEventContext().getMessage().setProperty(propertyName, methodName);

            ep.invoke(new FruitBowl(), RequestContext.getEventContext());
            fail("Should have failed to find an entrypoint.");
        }
        catch (NoSuchMethodException itex)
        {
            // expected
        }
        finally
        {
            RequestContext.setEvent(null);
        }
    }

    /**
     * If there was a method parameter specified to override the discovery mechanism
     * and a Callable instance is serving the request, call the Callable, ignore the
     * method override parameter.
     */
    public void testMethodOverrideIgnoredWithCallable() throws Exception
    {
        UMOEntryPointResolver epd = getEntryPointResolver();
        UMODescriptor descriptor = getDescriptorToResolve(Apple.class.getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try
        {
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
            ep.invoke(apple, RequestContext.getEventContext());
        }
        finally
        {
            RequestContext.setEvent(null);
        }
    }

    /**
     * If there was a method parameter specified to override the discovery mechanism
     * and a target instance has a method accepting UMOEventContext, proceed to call
     * this method, ignore the method override parameter.
     */
    public void testMethodOverrideIgnoredWithEventContext() throws Exception
    {
        UMOEntryPointResolver epd = getEntryPointResolver();
        UMODescriptor descriptor = getDescriptorToResolve(Kiwi.class.getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try
        {
            RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

            // those are usually set on the endpoint and copied over to the message
            final String methodName = "nosuchmethod";
            final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
            RequestContext.getEventContext().getMessage().setProperty(propertyName, methodName);

            try
            {
                ep.invoke(new Kiwi(), RequestContext.getEventContext());
                fail("no such method on component");
            }
            catch (NoSuchMethodException e)
            {
                // expected
            }
        }
        finally
        {
            RequestContext.setEvent(null);
        }
    }

    /**
     * Test for proper resolution of a method that takes an array as argument.
     */
    // TODO MULE-1088: currently fails, therefore disabled
    public void _testArrayArgumentResolution() throws Exception
    {
        UMOEntryPointResolver epd = this.getEntryPointResolver();
        UMODescriptor descriptor = this.getDescriptorToResolve(FruitBowl.class.getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try
        {
            Object payload = new Fruit[]{new Apple(), new Banana()};
            RequestContext.setEvent(getTestEvent(payload));

            FruitBowl bowl = new FruitBowl();
            assertFalse(bowl.hasApple());
            assertFalse(bowl.hasBanana());

            ep.invoke(bowl, RequestContext.getEventContext());

            assertTrue(bowl.hasApple());
            assertTrue(bowl.hasBanana());
        }
        finally
        {
            RequestContext.setEvent(null);
        }
    }

    /**
     * Test for proper resolution of a method that takes a List as argument.
     */
    public void testListArgumentResolution() throws Exception
    {
        UMOEntryPointResolver epd = this.getEntryPointResolver();
        UMODescriptor descriptor = this.getDescriptorToResolve(FruitBowl.class.getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try
        {
            Object payload = Arrays.asList(new Fruit[]{new Apple(), new Banana()});
            RequestContext.setEvent(getTestEvent(payload));

            FruitBowl bowl = new FruitBowl();
            assertFalse(bowl.hasApple());
            assertFalse(bowl.hasBanana());

            ep.invoke(bowl, RequestContext.getEventContext());

            assertTrue(bowl.hasApple());
            assertTrue(bowl.hasBanana());
        }
        finally
        {
            RequestContext.setEvent(null);
        }
    }

    /**
     * Test for proper resolution of an existing method specified as override
     */
    public void testExplicitOverride() throws Exception
    {
        UMOEntryPointResolver epd = this.getEntryPointResolver();
        UMODescriptor descriptor = this.getDescriptorToResolve(FruitBowl.class.getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try
        {
            Object payload = Arrays.asList(new Fruit[]{new Apple(), new Banana()});
            RequestContext.setEvent(getTestEvent(payload));

            final String methodName = "setFruit";
            final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
            RequestContext.getEventContext().getMessage().setProperty(propertyName, methodName);

            FruitBowl bowl = new FruitBowl();
            assertFalse(bowl.hasApple());
            assertFalse(bowl.hasBanana());

            ep.invoke(bowl, RequestContext.getEventContext());

            assertTrue(bowl.hasApple());
            assertTrue(bowl.hasBanana());
        }
        finally
        {
            RequestContext.setEvent(null);
        }
    }
}
