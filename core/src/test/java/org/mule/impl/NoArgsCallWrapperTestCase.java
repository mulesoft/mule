/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.components.simple.NoArgsCallWrapper;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ExceptionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NoArgsCallWrapperTestCase extends AbstractMuleTestCase
{
    public NoArgsCallWrapperTestCase()
    {
        setStartContext(true);
    }

    public void testNoArgsCallWrapper() throws Exception
    {
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateMethod", "toString");

        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupInboundEndpoint("test://in", managementContext);
        UMOComponent component = MuleTestUtils.getTestComponent("WrapperUMO", NoArgsCallWrapper.class, properties, managementContext);
        managementContext.getRegistry().registerComponent(component, managementContext);

        component = managementContext.getRegistry().lookupComponent("WrapperUMO");

        UMOEvent event = getTestEvent("Test", component, endpoint);
        UMOMessage reply = component.sendEvent(event);

        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        assertEquals("Just an apple.", reply.getPayload());
    }

    public void testVoidReturnType() throws Exception
    {
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateMethod", "wash");

        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupInboundEndpoint("test://in", managementContext);
        UMOComponent component = MuleTestUtils.getTestComponent("WrapperUMO", NoArgsCallWrapper.class, properties, managementContext);
        managementContext.getRegistry().registerComponent(component, managementContext);

        component = managementContext.getRegistry().lookupComponent("WrapperUMO");

        UMOEvent event = getTestEvent("Test", component, endpoint);
        UMOMessage reply = component.sendEvent(event);

        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        // same as original request
        assertEquals("Test", reply.getPayload());
    }

    public void testNullReturnType() throws Exception
    {
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateMethod", "methodReturningNull");

        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupInboundEndpoint("test://in", managementContext);
        UMOComponent component = MuleTestUtils.getTestComponent("WrapperUMO", NoArgsCallWrapper.class, properties, managementContext);
        managementContext.getRegistry().registerComponent(component, managementContext);

        component = managementContext.getRegistry().lookupComponent("WrapperUMO");

        UMOEvent event = getTestEvent("Test", component, endpoint);
        UMOMessage reply = component.sendEvent(event);
        
        assertNull(reply);
    }


    public void testNoConfigurationProvided() throws Exception
    {
        try
        {
            UMOComponent component = MuleTestUtils.getTestComponent("WrapperUMO", NoArgsCallWrapper.class, Collections.EMPTY_MAP, managementContext);
            managementContext.getRegistry().registerComponent(component, managementContext);
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("Both \"delegateClass\" and") > -1);
        }
    }

    public void testOnlyDelegateClassProvided() throws Exception
    {
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());

        try
        {
            UMOComponent component = MuleTestUtils.getTestComponent("WrapperUMO", NoArgsCallWrapper.class, properties, managementContext);
            managementContext.getRegistry().registerComponent(component, managementContext);
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("Both \"delegateClass\" and") > -1);
        }
    }

    public void testOnlyDelegateMethodProvided() throws Exception
    {
        Map properties = new HashMap();
        properties.put("delegateMethod", "someMethod");

        try
        {
            UMOComponent component = MuleTestUtils.getTestComponent("WrapperUMO", NoArgsCallWrapper.class, properties, managementContext);
            managementContext.getRegistry().registerComponent(component, managementContext);
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("Both \"delegateClass\" and") > -1);
        }
    }

    public void testDelegateInstanceAndClassProvided() throws Exception
    {
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateInstance", new Apple());

        try
        {
            UMOComponent component = MuleTestUtils.getTestComponent("WrapperUMO", NoArgsCallWrapper.class, properties, managementContext);
            managementContext.getRegistry().registerComponent(component, managementContext);
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("No \"delegateClass\" must be specified") > -1);
        }
    }

    public void testDelegateInstanceWithoutMethodProvided() throws Exception
    {
        Map properties = new HashMap();
        properties.put("delegateInstance", new Apple());

        try
        {
            UMOComponent component = MuleTestUtils.getTestComponent("WrapperUMO", NoArgsCallWrapper.class, properties, managementContext);
            managementContext.getRegistry().registerComponent(component, managementContext);
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("The required object/property") > -1);
        }
    }

}
