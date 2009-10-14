/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;


import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.lifecycle.AlreadyInitialisedException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import junit.framework.Assert;

/**
 * Tests that lifecycle methods on a connector are not processed more than once. (@see MULE-3062)
 * Also test lifecycle of a connector dispatchers, receivers, workManagers and scheduler.
 */
public class ConnectorLifecycleTestCase extends AbstractMuleTestCase 
{
    private TestConnector connector;

    @Override
    public void doSetUp() throws Exception
    {
        connector = new TestConnector();
        connector.setMuleContext(muleContext);
        connector.initialise();
    }

    @Override
    public void doTearDown() throws Exception
    {
        connector = null;
    }

    /**
     * This test ensures that the connector is only initialised once even on a
     * direct initialisation (not through Mule).
     */
    public void testDoubleInitialiseConnector() throws Exception
    {
        // Note: the connector was already initialized once during doSetUp()

        // Initialising the connector should leave it disconnected.
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());

        // Initialising the connector again should not throw an exception.
        try {
            System.out.println("Initialising connector again...");
            connector.initialise();
            Assert.fail("Expected AlreadyInitialisedException not thrown.");
        } catch (AlreadyInitialisedException ex) {
            // ignore since expected
        }
    }

    /**
     * This test ensures that the connector is only started once even on a
     * direct restart (not through Mule).
     */
    public void testDoubleStartConnector() throws Exception
    {
        // Starting the connector should leave it uninitialised,
        // but connected and started.
        System.out.println("Starting connector...");
        connector.start();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());

        // Starting the connector against should not affect it.
        System.out.println("Starting connector again...");
        connector.start();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());
    }

    /**
     * This test ensures that the connector is only stopped once even on a
     * direct restop (not through Mule).
     */
    public void testDoubleStopConnector() throws Exception
    {
        // Starting the connector should leave it uninitialised,
        // but connected and started.
        System.out.println("Starting connector...");
        connector.start();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());

        assertTrue(connector.isStarted());

        // Stopping the connector should stop and disconnect it.
        System.out.println("Stopping connector...");
        connector.stop();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());

        // Stopping the connector again should not affect it.
        System.out.println("Stopping connector again...");
        connector.stop();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());
    }

    /**
     * This test ensures that the connector is only disposed once even on a
     * direct disposal (not through Mule).
     */
    public void testDoubleDisposeConnectorStartStop() throws Exception
    {
        System.out.println("Starting connector...");
        connector.start();
        assertTrue(connector.isStarted());
        
        System.out.println("Stopping connector...");
        connector.stop();
        assertFalse(connector.isStarted());
        
        // Disposing the connector should leave it uninitialised.
        System.out.println("Disposing connector...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        // Disposing the connector again should not affect it.
        System.out.println("Disposing connector again...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());
    }

    /**
     * This test ensures that the connector is only disposed once even on a
     * direct disposal (not through Mule).
     */
    public void testDoubleDisposeConnectorStartOnly() throws Exception 
    {
        System.out.println("Starting connector...");
        connector.start();
        assertTrue(connector.isStarted());
        
        // Disposing the connector should leave it uninitialised.
        System.out.println("Disposing connector...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        // dispose() implicitly calls stop()
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        // Disposing the connector again should not affect it.
        System.out.println("Disposing connector again...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        // dispose() implicitly calls stop()
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());   
    }

    /**
     * This test ensures that the connector is only disposed once even on a
     * direct disposal (not through Mule).
     */
    public void testDoubleDisposeConnector() throws Exception 
    {
        // Disposing the connector should leave it uninitialised.
        System.out.println("Disposing connector...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        // Disposing the connector again should not affect it.
        System.out.println("Disposing connector again...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());
    }

    public void testReceiversLifecycle() throws Exception
    {
        connector.registerListener(getTestService(), getTestInboundEndpoint("in", "test://in"));

        assertEquals(1, connector.receivers.size());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        connector.start();
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        connector.registerListener(getTestService(), getTestInboundEndpoint("in2", "test://in2"));

        assertEquals(2, connector.receivers.size());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        // TODO MULE-4554 Receivers that are created (when new listener is registered) while connector is started are not started or connected
        // assertTrue(((AbstractMessageReceiver)connector.receivers.get("in2")).isConnected());
        // assertTrue(((AbstractMessageReceiver)connector.receivers.get("in2")).isStarted());

        connector.stop();
        assertEquals(2, connector.receivers.size());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in2")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in2")).isStarted());

        connector.start();
        assertEquals(2, connector.receivers.size());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in2")).isConnected());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in2")).isStarted());

        connector.dispose();
        assertEquals(0, connector.receivers.size());

    }

    public void testDispatchersLifecycle() throws Exception
    {
        OutboundEndpoint out = getTestOutboundEndpoint("out", "test://out");

        // TODO MULE-4264 Connectors should check lifecycle and throw exception when
        // attempts to send/dispatch/request are made on a stopped/stopping connector
        // This should fail because the connector is not started!
        connector.send(out, getTestEvent("data"));

        assertEquals(1, connector.dispatchers.getNumIdle());

        // Dispatcher is not started but it is connected because it has been used
        assertDispatcherStartedConntected(out, false, true); // Incorrect (assert current behavior)

        connector.start();

        // TODO MULE-4552 MessageDispatchers do not participate in connector
        // lifecycle
        // assertTrue(((AbstractMessageDispatcher)
        // connector.dispatchers.borrowObject(out)).isStarted());
        // assertTrue(((AbstractMessageDispatcher)
        // connector.dispatchers.borrowObject(out)).isConnected());
        assertDispatcherStartedConntected(out, false, true); // Incorrect (assert current behavior)

        OutboundEndpoint out2 = getTestOutboundEndpoint("out2", "test://out2");
        connector.send(out2, getTestEvent("data"));

        assertEquals(2, connector.dispatchers.getNumIdle());
        // TODO MULE-4552 MessageDispatchers do not participate in connector
        // lifecycle
        // assertDispatcherStartedConntected(out, true, true);
        // assertDispatcherStartedConntected(out2, true, true);
        assertDispatcherStartedConntected(out, false, true); // Incorrect (assert current behavior)
        assertDispatcherStartedConntected(out2, false, true); // Incorrect (assert current behavior)

        connector.stop();
        // Pool is cleared because of implementation of workaround for MULE-4553
        assertEquals(0, connector.dispatchers.getNumActive() + connector.dispatchers.getNumIdle());
        // TODO MULE-4552 MessageDispatchers do not participate in connector
        // lifecycle
        // assertDispatcherStartedConntected(out, false, false);
        // assertDispatcherStartedConntected(out2, false, false);

        connector.start();
        assertEquals(0, connector.dispatchers.getNumActive() + connector.dispatchers.getNumIdle());
        // TODO MULE-4552 MessageDispatchers do not participate in connector
        // lifecycle
        // assertDispatcherStartedConntected(out, true, true);
        // assertDispatcherStartedConntected(out2, true, true);

        connector.send(out, getTestEvent("data"));
        assertEquals(1, connector.dispatchers.getNumIdle());
        assertDispatcherStartedConntected(out, false, true); // Incorrect (assert current behavior)

        connector.dispose();
        assertEquals(0, connector.dispatchers.getNumActive() + connector.dispatchers.getNumIdle());

    }

    public void testWorkManagerLifecycle() throws MuleException, WorkException
    {
        assertNull(connector.getReceiverWorkManager(null));
        assertNull(connector.getDispatcherWorkManager());
        assertNull(connector.getRequesterWorkManager());

        connector.start();
        assertNotNull(connector.getReceiverWorkManager(null));
        assertNotNull(connector.getDispatcherWorkManager());
        assertNotNull(connector.getRequesterWorkManager());
        connector.getReceiverWorkManager(null).doWork(createSomeWork());
        connector.getDispatcherWorkManager().doWork(createSomeWork());
        connector.getRequesterWorkManager().doWork(createSomeWork());

        connector.stop();
        assertNull(connector.getReceiverWorkManager(null));
        assertNull(connector.getDispatcherWorkManager());
        assertNull(connector.getRequesterWorkManager());

        connector.start();
        assertNotNull(connector.getReceiverWorkManager(null));
        assertNotNull(connector.getDispatcherWorkManager());
        assertNotNull(connector.getRequesterWorkManager());
        connector.getReceiverWorkManager(null).doWork(createSomeWork());
        connector.getDispatcherWorkManager().doWork(createSomeWork());
        connector.getRequesterWorkManager().doWork(createSomeWork());

        connector.dispose();
        assertNull(connector.getReceiverWorkManager(null));
        assertNull(connector.getDispatcherWorkManager());
        assertNull(connector.getRequesterWorkManager());
    }

    public void testSchedulerLifecycle() throws MuleException, WorkException
    {
        // Scheduler lifecycle is hard to test because scheduler is created in
        // getScheduler() method and the field is private. (MULE-4555)

        assertFalse(connector.getScheduler().isShutdown());
        assertFalse(connector.getScheduler().isTerminated());

        connector.start();
        assertFalse(connector.getScheduler().isShutdown());
        assertFalse(connector.getScheduler().isTerminated());

        connector.stop();
        assertFalse(connector.getScheduler().isShutdown());
        assertFalse(connector.getScheduler().isTerminated());

        connector.start();
        assertFalse(connector.getScheduler().isShutdown());
        assertFalse(connector.getScheduler().isTerminated());

        connector.dispose();
        assertFalse(connector.getScheduler().isShutdown());
        assertFalse(connector.getScheduler().isTerminated());
    }

    protected Work createSomeWork()
    {
        return new Work()
        {
            public void run()
            {
                System.out.println("I'm doing some work");
            }

            public void release()
            {
            }
        };
    }

    private void assertDispatcherStartedConntected(OutboundEndpoint out, boolean started, boolean connected)
        throws Exception
    {
        AbstractMessageDispatcher dispatcher = (AbstractMessageDispatcher) connector.dispatchers.borrowObject(out);
        assertEquals(started, dispatcher.isStarted());
        assertEquals(connected, dispatcher.isConnected());
        connector.dispatchers.returnObject(out, dispatcher);
    }

}
