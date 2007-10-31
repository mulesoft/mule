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

import org.mule.components.simple.EchoComponent;
import org.mule.providers.AbstractConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.provider.UMOMessageReceiver;

import java.util.Iterator;

public class MuleComponentTestCase extends AbstractMuleTestCase
{
    public MuleComponentTestCase()
    {
        setStartContext(true);
    }
    
    public void testSendToPausedComponent() throws Exception
    {
        UMOComponent c = MuleTestUtils.getTestComponent("orangeComponent", Orange.class, null, managementContext, false);
        managementContext.getRegistry().registerComponent(c, managementContext);

        // TODO MULE-1995
        final UMOComponent comp = managementContext.getRegistry().lookupComponent("orangeComponent");
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
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
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
        UMOComponent comp = MuleTestUtils.getTestComponent("myComponent", EchoComponent.class, null, managementContext, false);
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
            comp.sendEvent(getTestEvent("hello", comp));
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
        // Test connector
        getTestConnector();
        // Test component
        UMOComponent c = MuleTestUtils.getTestComponent("orangeComponent", Orange.class, null, managementContext, false);
        c.setInitialState(ImmutableMuleDescriptor.INITIAL_STATE_STOPPED);
        managementContext.getRegistry().registerComponent(c, managementContext);

        // TODO MULE-1995
        c = managementContext.getRegistry().lookupComponent("orangeComponent");
        // Component initially stopped
        assertFalse(c.isStarted());

        // The connector should be started, but with no listeners registered.
        AbstractConnector connector = (AbstractConnector)managementContext.getRegistry().lookupConnector("testConnector");
        assertNotNull(connector);
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
        // Test connector
        getTestConnector();
        // Test component
        UMOComponent c = MuleTestUtils.getTestComponent("orangeComponent", Orange.class, null, managementContext, false);
        managementContext.getRegistry().registerComponent(c, managementContext);

        // TODO MULE-1995
        c = managementContext.getRegistry().lookupComponent("orangeComponent");
        assertTrue(c.isStarted());

        // The listeners should be registered and started.
        AbstractConnector connector = (AbstractConnector)managementContext.getRegistry().lookupConnector("testConnector");
        assertNotNull(connector);
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
