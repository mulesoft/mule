/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.test.mule.model;

import org.mule.impl.RequestContext;
import org.mule.model.DynamicEntryPointResolver;
import org.mule.model.TooManySatisfiableMethodsException;
import org.mule.model.NoSatisfiableMethodsException;
import org.mule.tck.model.AbstractEntryPointDiscoveryTestCase;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitLover;
import org.mule.tck.testmodels.fruit.ObjectToFruitLover;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.config.MuleProperties;

import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 *
 * @version $Revision$
 */
public class ExtendedEntryPointDiscoveryTestCase extends AbstractEntryPointDiscoveryTestCase
{
    /** Name of the method override property on the event. */
    private static final String METHOD_PROPERTY_NAME = MuleProperties.MULE_METHOD_PROPERTY;

    /** Name of the non-existent method. */
    private static final String INVALID_METHOD_NAME = "nosuchmethod";

    /*
    * (non-Javadoc)
    *
    * @see org.mule.tck.model.AbstractEntryPointDiscoveryTestCase#getComponentMappings()
    */
    public ComponentMethodMapping[] getComponentMappings()
    {
        ComponentMethodMapping[] mappings = new ComponentMethodMapping[3];
        mappings[0] = new ComponentMethodMapping(WaterMelon.class, "myEventHandler", UMOEvent.class);
        mappings[1] = new ComponentMethodMapping(FruitBowl.class, "consumeFruit", FruitLover.class);
        mappings[2] = new ComponentMethodMapping(Banana.class, "peelEvent", EventObject.class);
        return mappings;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.tck.model.AbstractEntryPointDiscoveryTestCase#getDescriptorToResolve(java.lang.String)
     */
    public UMODescriptor getDescriptorToResolve(String className) throws Exception
    {
        UMODescriptor descriptor = super.getDescriptorToResolve(className);
        if (className.equals(FruitBowl.class.getName())) {
            UMOEndpoint endpoint = descriptor.getOutboundEndpoint();
            endpoint.setType(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER);
            endpoint.setTransformer(new ObjectToFruitLover());
            descriptor.setInboundEndpoint(endpoint);
        }
        return descriptor;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.tck.model.AbstractEntryPointDiscoveryTestCase#getEntryPointResolver()
     */
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
        UMODescriptor descriptor = getTestDescriptor("badContexts", MultipleEventContextsTestObject.class.getName());

        UMOEntryPoint ep;
        ep = epd.resolveEntryPoint(descriptor);
        assertTrue(ep != null);
        try {

            RequestContext.setEvent(getTestEvent("Hello"));
            ep.invoke(new MultipleEventContextsTestObject(), RequestContext.getEventContext());
            fail("Should have failed to find entrypoint.");
        } catch (InvocationTargetException itex) {
            final Throwable cause = itex.getCause();
            if (cause instanceof TooManySatisfiableMethodsException) {
                // expected
            } else {
                throw itex;
            }
        } finally {
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
        UMODescriptor descriptor = getTestDescriptor("badPayloads", MultiplePayloadsTestObject.class.getName());

        UMOEntryPoint ep;
        ep = epd.resolveEntryPoint(descriptor);
        assertTrue(ep != null);
        try {

            RequestContext.setEvent(getTestEvent("Hello"));
            ep.invoke(new MultiplePayloadsTestObject(), RequestContext.getEventContext());
            fail("Should have failed to find entrypoint.");

        } catch (TooManySatisfiableMethodsException itex) {
            // expected
        } finally {
            RequestContext.setEvent(null);
        }

    }

    /**
     * If there was a method parameter specified to override the discovery
     * mechanism and no such method exists, an exception should be thrown,
     * and no fallback to the default discovery should take place.
     */
    public void testMethodOverrideDoesNotFallback() throws Exception
    {
        UMOEntryPointResolver epd = getEntryPointResolver();
        UMODescriptor descriptor = getDescriptorToResolve(FruitBowl.class.getName());

        UMOEntryPoint ep = null;
        ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try {

            RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

            // those are usually set on the endpoint and copied over to the message
            final String methodName = "nosuchmethod";
            final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
            RequestContext.getEventContext().getMessage().setProperty(propertyName, methodName);

            ep.invoke(new FruitBowl(), RequestContext.getEventContext());
            fail("Should have failed to find an entrypoint.");
        } catch (NoSuchMethodException itex) {
            //expected
        } finally {
            RequestContext.setEvent(null);
        }
    }

    /**
     * If there was a method parameter specified to override the discovery
     * mechanism and a Callable instance is serving the request, call
     * the Callable, ignore the method override parameter.
     */
    public void testMethodOverrideIgnoredWithCallable() throws Exception
    {
        UMOEntryPointResolver epd = getEntryPointResolver();
        UMODescriptor descriptor = getDescriptorToResolve(Apple.class.getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try {

            RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

            // those are usually set on the endpoint and copied over to the message
            RequestContext.getEventContext().getMessage().setProperty(METHOD_PROPERTY_NAME, INVALID_METHOD_NAME);

            ep.invoke(new Apple(), RequestContext.getEventContext());
        } finally {
            RequestContext.setEvent(null);
        }
    }

    /**
     * If there was a method parameter specified to override the discovery
     * mechanism and a target instance has a method accepting UMOEventContext,
     * proceed to call this method, ignore the method override parameter.
     */
    public void testMethodOverrideIgnoredWithEventContext() throws Exception
    {
        UMOEntryPointResolver epd = getEntryPointResolver();
        UMODescriptor descriptor = getDescriptorToResolve(Kiwi.class.getName());

        UMOEntryPoint ep = epd.resolveEntryPoint(descriptor);
        assertNotNull(ep);

        try {

            RequestContext.setEvent(getTestEvent(new FruitLover("Yummy!")));

            // those are usually set on the endpoint and copied over to the message
            final String methodName = "nosuchmethod";
            final String propertyName = MuleProperties.MULE_METHOD_PROPERTY;
            RequestContext.getEventContext().getMessage().setProperty(propertyName, methodName);

            try {
                ep.invoke(new Kiwi(), RequestContext.getEventContext());
                fail("no such method on component");
            } catch (NoSuchMethodException e) {
               //expected
            }
        } finally {
            RequestContext.setEvent(null);
        }
    }
}
