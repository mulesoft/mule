/*
 * $Id: MuleComponentTestCase.java 9503 2007-10-31 12:41:22Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components;

import org.mule.providers.AbstractConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.provider.UMOMessageReceiver;

public class ComponentInitialStateTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/components/component-initial-state.xml";
    }

    public ComponentInitialStateTestCase()
    {
        setStartContext(true);
    }
    
    public void testDefaultInitialState() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("defaultComponent");
        // Component initially started
        assertTrue(c.isStarted());
        assertFalse(c.isPaused());

        // The listeners should be registered and started.
        AbstractConnector connector = (AbstractConnector)managementContext.getRegistry().lookupConnector("connector.test.0");
        assertNotNull(connector);
        assertTrue(connector.isStarted());
        UMOMessageReceiver[] receivers = connector.getReceivers("*default*");
        assertEquals(1, receivers.length);
        assertTrue(receivers[0].isConnected());
    }

    // MULE-494
    public void testInitialStateStopped() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("stoppedComponent");
        // Component initially stopped
        assertFalse(c.isStarted());

        // The connector should be started, but with no listeners registered.
        AbstractConnector connector = (AbstractConnector)managementContext.getRegistry().lookupConnector("connector.test.0");
        assertNotNull(connector);
        assertTrue(connector.isStarted());
        UMOMessageReceiver[] receivers = connector.getReceivers("*stopped*");
        assertEquals(0, receivers.length);

        // Start the component.
        c.start();
        assertTrue(c.isStarted());

        // The listeners should now be registered and started.
        assertTrue(connector.isStarted());
        receivers = connector.getReceivers("*stopped*");
        assertEquals(1, receivers.length);
        assertTrue(receivers[0].isConnected());
    }

    // MULE-503
    public void testStoppingComponentStopsEndpoints() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("startedComponent");
        assertTrue(c.isStarted());

        // The listeners should be registered and started.
        AbstractConnector connector = (AbstractConnector)managementContext.getRegistry().lookupConnector("connector.test.0");
        assertNotNull(connector);
        assertTrue(connector.isStarted());
        UMOMessageReceiver[] receivers = connector.getReceivers("*started*");
        assertEquals(1, receivers.length);
        assertTrue(receivers[0].isConnected());

        // Stop component
        c.stop();
        assertFalse(c.isStarted());

        // Connector is still started, but no more receivers.
        assertTrue(connector.isStarted());
        receivers = connector.getReceivers("*started*");
        assertEquals(0, receivers.length);
    }
    
    public void testSendToStoppedComponent() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("stoppedComponent");
        // Component initially stopped
        assertFalse(c.isStarted());

        try
        {
            c.dispatchEvent(getTestEvent("hello", c));
            fail();
        }
        catch (ComponentException e)
        {
            // expected
        }

        try
        {
            c.sendEvent(getTestEvent("hello", c));
            fail();
        }
        catch (ComponentException e)
        {
            // expected
        }
    }

    public void testInitialStatePaused() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("pausedComponent");
        // Component initially started but paused.
        assertTrue(c.isStarted());
        assertTrue(c.isPaused());

        // The listeners should be registered and started.
        AbstractConnector connector = (AbstractConnector)managementContext.getRegistry().lookupConnector("connector.test.0");
        assertNotNull(connector);
        assertTrue(connector.isStarted());
        UMOMessageReceiver[] receivers = connector.getReceivers("*paused*");
        assertEquals(1, receivers.length);
        assertTrue(receivers[0].isConnected());
    }

    public void testSendToPausedComponent() throws Exception
    {
        // TODO MULE-1995
        final UMOComponent c = managementContext.getRegistry().lookupComponent("startedComponent");
        assertTrue(c.isStarted());
        
        c.pause();
        assertTrue(c.isPaused());
        
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
                    c.resume();
                }
                catch (UMOException e)
                {
                    fail(e.getMessage());
                }
            }
        }).start();
        long t0 = System.currentTimeMillis();
        c.sendEvent(getTestEvent("hello"));
        long t1 = System.currentTimeMillis();
        assertTrue(t1 - t0 > 1000);
    }
}
