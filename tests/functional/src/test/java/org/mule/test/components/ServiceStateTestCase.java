/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.AbstractConnector;

import org.junit.Test;

public class ServiceStateTestCase extends FunctionalTestCase
{
    public ServiceStateTestCase()
    {
        setStartContext(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/components/component-initial-state.xml";
    }

    @Test
    public void testDefaultInitialState() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("defaultComponent");
        // Service initially started
        assertTrue(c.isStarted());
        assertFalse(c.isPaused());
        assertFalse(c.isStopped());

        // The listeners should be registered and started.
        AbstractConnector connector = (AbstractConnector)muleContext.getRegistry().lookupConnector("connector.test.mule.default");
        assertNotNull(connector);
        assertTrue(connector.isStarted());
        MessageReceiver[] receivers = connector.getReceivers("*default*");
        assertEquals(1, receivers.length);
        assertTrue(receivers[0].isConnected());
    }

    // MULE-494
    @Test
    public void testInitialStateStopped() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("stoppedComponent");
        assertEquals("stopped", c.getInitialState());
        // Service initially stopped
        assertFalse(c.isStarted());
        assertTrue(c.isStopped());
        assertFalse(c.isPaused());

        // The connector should be started, but with no listeners registered.
        AbstractConnector connector = (AbstractConnector)muleContext.getRegistry().lookupConnector("connector.test.mule.default");
        assertNotNull(connector);
        assertTrue(connector.isStarted());
        MessageReceiver[] receivers = connector.getReceivers("*stopped*");
        assertEquals(0, receivers.length);

        // Start the service.
        c.start();
        assertTrue(c.isStarted());
        assertFalse(c.isStopped());
        assertFalse(c.isPaused());

        // The listeners should now be registered and started.
        assertTrue(connector.isStarted());
        receivers = connector.getReceivers("*stopped*");
        assertEquals(1, receivers.length);
        assertTrue(receivers[0].isConnected());
    }

    // MULE-503
    @Test
    public void testStoppingComponentStopsEndpoints() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("startedComponent");
        assertTrue(c.isStarted());
        assertFalse(c.isStopped());
        assertFalse(c.isPaused());

        // The listeners should be registered and started.
        AbstractConnector connector = (AbstractConnector)muleContext.getRegistry().lookupConnector("connector.test.mule.default");
        assertNotNull(connector);
        assertTrue(connector.isStarted());
        MessageReceiver[] receivers = connector.getReceivers("*started*");
        assertEquals(1, receivers.length);
        assertTrue(receivers[0].isConnected());

        // Stop service
        c.stop();
        assertFalse(c.isStarted());
        assertFalse(c.isPaused());
        assertTrue(c.isStopped());

        // Connector is still started, but no more receivers.
        assertTrue(connector.isStarted());
        receivers = connector.getReceivers("*started*");
        assertEquals(0, receivers.length);
    }
    
    @Test
    public void testSendToStoppedComponent() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("stoppedComponent");
        // Service initially stopped
        assertFalse(c.isStarted());
        assertFalse(c.isPaused());
        assertTrue(c.isStopped());

        try
        {
            c.dispatchEvent(getTestEvent("hello", c));
            fail();
        }
        catch (MessagingException e)
        {
            assertTrue(e.getCause() instanceof LifecycleException);
        }

        try
        {
            c.sendEvent(getTestEvent("hello", c));
            fail();
        }
        catch (MessagingException e)
        {
            assertTrue(e.getCause() instanceof LifecycleException);
        }
    }

    @Test
    public void testInitialStatePaused() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("pausedComponent");
        // Service initially started but paused.
        assertFalse(c.isStarted());
        assertTrue(c.isPaused());
        assertFalse(c.isStopped());

        // The listeners should be registered and started.
        AbstractConnector connector = (AbstractConnector)muleContext.getRegistry().lookupConnector("connector.test.mule.default");
        assertNotNull(connector);
        assertTrue(connector.isStarted());
        MessageReceiver[] receivers = connector.getReceivers("*paused*");
        assertEquals(1, receivers.length);
        assertTrue(receivers[0].isConnected());
    }

    @Test
    public void testSendToPausedComponent() throws Exception
    {
        // TODO MULE-1995
        final Service c = muleContext.getRegistry().lookupService("startedComponent");
        assertTrue(c.isStarted());
        assertFalse(c.isPaused());
        assertFalse(c.isStopped());
        
        c.pause();
        assertTrue(c.isPaused());
        assertFalse(c.isStopped());
        assertFalse(c.isStarted());
        
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
                catch (MuleException e)
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
