/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.providers.AbstractConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOSession;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOMessageReceiver;

import java.util.Iterator;

public class MuleComponentTestCase extends AbstractMuleTestCase
{
    public void testSendToPausedComponent() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        builder.registerComponent(Orange.class.getName(), "orangeComponent", "test://in", "test://out", null);
        builder.createStartedManager(true, "");
        UMOModel model = getDefaultModel();

        final UMOComponent comp = model.getComponent("orangeComponent");
        assertTrue(comp.isStarted());
        comp.pause();
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
                try
                {
                    comp.resume();
                }
                catch (UMOException e)
                {
                    fail(e.getMessage());
                }
            }
        }).start();
        long t0 = System.currentTimeMillis();
        comp.sendEvent(getTestEvent("hello"));
        long t1 = System.currentTimeMillis();
        assertTrue(t1 - t0 > 1000);
    }

    public void testSendToStoppedComponent() throws Exception
    {
        MuleDescriptor descriptor = getTestDescriptor("myComponent",
            "org.mule.components.simple.EchoComponent");
        UMOComponent comp = getTestComponent(descriptor);
        // Component is stopped because it has not been registered.
        assertTrue(!comp.isStarted());

        try
        {
            comp.dispatchEvent(getTestEvent("hello"));
            fail();
        }
        catch (ComponentException e)
        {
            // expected
        }

        try
        {
            comp.sendEvent(getTestEvent("hello"));
            fail();
        }
        catch (ComponentException e)
        {
            // expected
        }
    }

    // MULE-494
    public void testInitialStateStopped() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        // Test connector
        builder.getManager().registerConnector(getTestConnector());
        // Test component
        UMODescriptor d = builder.createDescriptor(Orange.class.getName(), "orangeComponent",
            builder.createEndpoint("test://in", null, true), null, null);
        d.setInitialState(ImmutableMuleDescriptor.INITIAL_STATE_STOPPED);
        builder.registerComponent(d);

        // Start model
        UMOManager manager = builder.createStartedManager(true, "");

        UMOModel model = getDefaultModel();
        UMOSession session = new MuleSession(model.getComponent("orangeComponent"));
        final UMOComponent c = session.getComponent();
        // Component initially stopped
        assertFalse(c.isStarted());

        // The connector should be started, but with no listeners registered.
        AbstractConnector connector = (AbstractConnector)manager.lookupConnector("testConnector");
        assertTrue(connector.isStarted());
        assertTrue(connector.getReceivers().isEmpty());

        // Start the component.
        c.start();

        // The listeners should now be registered and started.
        assertTrue(connector.isStarted());
        assertFalse(connector.getReceivers().isEmpty());

        assertTrue(c.isStarted());
        Iterator it = connector.getReceivers().values().iterator();
        while (it.hasNext())
        {
            assertTrue(((UMOMessageReceiver)it.next()).isConnected());
        }
    }

    // MULE-503
    public void testStoppingComponentStopsEndpoints() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        // Test connector
        builder.getManager().registerConnector(getTestConnector());
        // Test component
        builder.registerComponent(Orange.class.getName(), "orangeComponent", "test://in", "test://out", null);

        // Start model
        UMOManager manager = builder.createStartedManager(true, "");
        UMOModel model = getDefaultModel();
        UMOSession session = new MuleSession(model.getComponent("orangeComponent"));
        final UMOComponent c = session.getComponent();
        assertTrue(c.isStarted());

        // The listeners should be registered and started.
        AbstractConnector connector = (AbstractConnector)manager.lookupConnector("testConnector");
        assertTrue(connector.isStarted());
        assertFalse(connector.getReceivers().isEmpty());

        Iterator it = connector.getReceivers().values().iterator();
        while (it.hasNext())
        {
            assertTrue(((UMOMessageReceiver)it.next()).isConnected());
        }

        // Stop component
        c.stop();

        assertFalse(c.isStarted());
        // Connector is still started, but no more receivers.
        assertTrue(connector.isStarted());
        assertTrue(connector.getReceivers().isEmpty());
    }
}
