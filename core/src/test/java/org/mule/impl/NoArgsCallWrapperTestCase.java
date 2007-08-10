package org.mule.impl;

import org.mule.components.simple.NoArgsCallWrapper;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ExceptionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NoArgsCallWrapperTestCase extends AbstractMuleTestCase
{

    public void testNoArgsCallWrapper() throws Exception
    {
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateMethod", "toString");

        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        UMOEndpoint endpoint = builder.createEndpoint("test://in", null, true);
        MuleDescriptor desc = (MuleDescriptor) builder.createDescriptor(NoArgsCallWrapper.class.getName(), "WrapperUMO", endpoint, null, properties);
        builder.registerComponent(desc);

        UMOComponent component = builder.getModel().getComponent("WrapperUMO");

        UMOEvent event = getTestEvent("Test", desc, endpoint);
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

        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        UMOEndpoint endpoint = builder.createEndpoint("test://in", null, true);
        MuleDescriptor desc = (MuleDescriptor) builder.createDescriptor(NoArgsCallWrapper.class.getName(), "WrapperUMO", endpoint, null, properties);
        builder.registerComponent(desc);

        UMOComponent component = builder.getModel().getComponent("WrapperUMO");

        UMOEvent event = getTestEvent("Test", desc, endpoint);
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

        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        UMOEndpoint endpoint = builder.createEndpoint("test://in", null, true);
        MuleDescriptor desc = (MuleDescriptor) builder.createDescriptor(NoArgsCallWrapper.class.getName(), "WrapperUMO", endpoint, null, properties);
        builder.registerComponent(desc);

        UMOComponent component = builder.getModel().getComponent("WrapperUMO");

        UMOEvent event = getTestEvent("Test", desc, endpoint);
        UMOMessage reply = component.sendEvent(event);
        
        assertNull(reply);
    }


    public void testNoConfigurationProvided() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);

        try
        {
            builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, Collections.EMPTY_MAP);
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
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());

        try
        {
            builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
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
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        Map properties = new HashMap();
        properties.put("delegateMethod", "someMethod");

        try
        {
            builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
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
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);

        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateInstance", new Apple());

        try
        {
            builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
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
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);

        Map properties = new HashMap();
        properties.put("delegateInstance", new Apple());

        try
        {
            builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
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
