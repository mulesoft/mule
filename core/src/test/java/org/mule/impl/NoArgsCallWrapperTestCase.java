package org.mule.impl;

import org.mule.components.simple.NoArgsCallWrapper;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ExceptionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NoArgsCallWrapperTestCase extends AbstractMuleTestCase
{

    public void testNoArgsCallWrapper() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateMethod", "toString");

        builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
        managementContext.start();

        MuleDescriptor desc = (MuleDescriptor) managementContext.getRegistry().lookupService("WrapperUMO");
        UMOEvent event = getTestEvent("Test", desc, new MuleEndpoint("test://in", true));
        UMOMessage reply = event.getComponent().sendEvent(event);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        assertEquals("Just an apple.", reply.getPayload());
    }

    public void testVoidReturnType() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateMethod", "wash");
        /*
        UMOComponent comp = builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
        builder.createStartedManager(true, "");

        UMOEvent event = getTestEvent("Test", (MuleDescriptor) comp.getDescriptor(), new MuleEndpoint("test://in", true));
        UMOMessage reply = comp.sendEvent(event);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        // same as original request
        assertEquals("Test", reply.getPayload());
        */
    }

    public void testNullReturnType() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateMethod", "methodReturningNull");

        /*
        UMOComponent comp = builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
        builder.createStartedManager(true, "");

        UMOEvent event = getTestEvent("Test", (MuleDescriptor) comp.getDescriptor(), new MuleEndpoint("test://in", true));
        UMOMessage reply = comp.sendEvent(event);
        assertNull(reply);
        */
    }


    public void testNoConfigurationProvided() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, Collections.EMPTY_MAP);
        /*
        try
        {
            builder.createStartedManager(true, "");
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("Both \"delegateClass\" and") > -1);
        }
        */
    }

    public void testOnlyDelegateClassProvided() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);

        /*try
        {
            builder.createStartedManager(true, "");
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("Both \"delegateClass\" and") > -1);
        }*/
    }

    public void testOnlyDelegateMethodProvided() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        Map properties = new HashMap();
        properties.put("delegateMethod", "someMethod");
        builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
        /*try
        {
            builder.createStartedManager(true, "");
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("Both \"delegateClass\" and") > -1);
        }*/
    }

    public void testDelegateInstanceAndClassProvided() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);

        Map properties = new HashMap();
        properties.put("delegateClass", Apple.class.getName());
        properties.put("delegateInstance", new Apple());

        builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
        /*try
        {
            builder.createStartedManager(true, "");
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("No \"delegateClass\" must be specified") > -1);
        }*/
    }

    public void testDelegateInstanceWithoutMethodProvided() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);

        Map properties = new HashMap();
        properties.put("delegateInstance", new Apple());

        builder.registerComponent(NoArgsCallWrapper.class.getName(), "WrapperUMO", "test://in", null, properties);
        /*try
        {
            builder.createStartedManager(true, "");
        }
        catch (UMOException e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue(t instanceof InitialisationException);
            assertTrue("Wrong exception?", t.getMessage().indexOf("The required object/property") > -1);
        }*/
    }

}
