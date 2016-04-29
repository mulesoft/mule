/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.transport.MessageDispatcher;
import org.mule.runtime.core.api.transport.MessageRequester;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.transport.AbstractMessageDispatcher;
import org.mule.runtime.core.transport.AbstractMessageReceiver;
import org.mule.runtime.core.transport.AbstractMessageRequester;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests that lifecycle methods on a connector are not processed more than once. (@see MULE-3062)
 * Also test lifecycle of a connector dispatchers, receivers, workManagers and scheduler.
 */
public class ConnectorLifecycleTestCase extends AbstractMuleContextTestCase
{

    private TestConnector connector;

    @Override
    public void doSetUp() throws Exception
    {
        connector = new TestConnector(muleContext);
        connector.initialise();
    }

    @Override
    public void doTearDown() throws Exception
    {
        if (!connector.isDisposed())
        {
            connector.dispose();
        }
        connector = null;
    }

    /**
     * This test ensures that the connector is only initialised once even on a
     * direct initialisation (not through Mule).
     *
     * @throws Exception if things go pear-shaped
     */
    @Test
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
        try
        {
            connector.initialise();
            Assert.fail("Expected IllegalStateException not thrown.");
        }
        catch (IllegalStateException ex)
        {
            // ignore since expected
        }
    }

    /**
     * This test ensures that the connector is only started once even on a
     * direct restart (not through Mule).
     *
     * @throws Exception if things go pear-shaped
     */
    @Test
    public void testDoubleStartConnector() throws Exception
    {
        // Starting the connector should leave it uninitialised,
        // but connected and started.
        connector.start();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());

        // Starting the connector again
        try
        {
            connector.start();
            fail("cannot start the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
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
     *
     * @throws Exception if things go pear-shaped
     */
    @Test
    public void testDoubleStopConnector() throws Exception
    {
        // Starting the connector should leave it uninitialised,
        // but connected and started.
        connector.start();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());

        assertTrue(connector.isStarted());

        // Stopping the connector should stop but not disconnect it.
        connector.stop();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());


        try
        {
            connector.stop();
            fail("cannot stop the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }

        connector.disconnect();
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
     *
     * @throws Exception if things go pear-shaped
     */
    @Test
    public void testDoubleDisposeConnectorStartStop() throws Exception
    {
        connector.start();
        assertTrue(connector.isStarted());

        connector.stop();
        assertFalse(connector.isStarted());

        // Disposing the connector should leave it uninitialised.
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        try
        {
            connector.dispose();
            fail("cannot dispose the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
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
     *
     * @throws Exception if things go pear-shaped
     */
    @Test
    public void testDoubleDisposeConnectorStartOnly() throws Exception
    {
        connector.start();
        assertTrue(connector.isStarted());

        // Disposing the connector should leave it uninitialised.
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        // dispose() implicitly calls stop()
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        try
        {
            connector.dispose();
            fail("cannot dispose the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
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
     *
     * @throws Exception if things go pear-shaped
     */
    @Test
    public void testDoubleDisposeConnector() throws Exception
    {
        // Disposing the connector should leave it uninitialised.
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        try
        {
            connector.dispose();
            fail("cannot dispose the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());
    }

    @Test
    public void testReceiversLifecycle() throws Exception
    {
        Flow flow = getTestFlow();
        flow.start();
        try
        {
            connector.registerListener(getTestInboundEndpoint("in", "test://in"), getSensingNullMessageProcessor(), flow);

            assertEquals(1, connector.receivers.size());
            assertFalse((connector.receivers.get("in")).isConnected());
            assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

            connector.start();
            assertTrue((connector.receivers.get("in")).isConnected());
            assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

            connector.registerListener(getTestInboundEndpoint("in2", "test://in2"), getSensingNullMessageProcessor(), flow);

            assertEquals(2, connector.receivers.size());
            assertTrue((connector.receivers.get("in")).isConnected());
            assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

            assertTrue((connector.receivers.get("in2")).isConnected());
            assertTrue(((AbstractMessageReceiver) connector.receivers.get("in2")).isStarted());

            connector.stop();
            assertEquals(2, connector.receivers.size());
            assertTrue((connector.receivers.get("in")).isConnected());
            assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());
            assertTrue((connector.receivers.get("in2")).isConnected());
            assertFalse(((AbstractMessageReceiver) connector.receivers.get("in2")).isStarted());

            connector.disconnect();
            assertEquals(2, connector.receivers.size());
            assertFalse((connector.receivers.get("in")).isConnected());
            assertFalse((connector.receivers.get("in2")).isConnected());

            connector.start();
            assertEquals(2, connector.receivers.size());
            assertTrue((connector.receivers.get("in")).isConnected());
            assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());
            assertTrue((connector.receivers.get("in2")).isConnected());
            assertTrue(((AbstractMessageReceiver) connector.receivers.get("in2")).isStarted());

            connector.dispose();
            assertEquals(0, connector.receivers.size());
        }
        finally
        {
            flow.dispose();
        }
    }

    @Test
    public void testReceiversServiceLifecycle() throws Exception
    {
        Flow flow = MuleTestUtils.getTestFlow(MuleTestUtils.APPLE_SERVICE, muleContext, false);
        InboundEndpoint endpoint = getTestInboundEndpoint("in", "test://in");
        flow.setMessageSource(endpoint);
        connector = (TestConnector) endpoint.getConnector();

        assertEquals(0, connector.receivers.size());

        connector.start();
        assertEquals(0, connector.receivers.size());

        flow.initialise();
        flow.start();
        assertEquals(1, connector.receivers.size());
        assertTrue((connector.receivers.get("in")).isConnected());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        connector.stop();
        assertEquals(1, connector.receivers.size());
        assertTrue((connector.receivers.get("in")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        connector.disconnect();
        assertEquals(1, connector.receivers.size());
        assertFalse((connector.receivers.get("in")).isConnected());

        connector.start();
        assertEquals(1, connector.receivers.size());
        assertTrue((connector.receivers.get("in")).isConnected());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        flow.stop();
        assertEquals(0, connector.receivers.size());

        connector.stop();
        assertEquals(0, connector.receivers.size());
    }

    @Test
    public void testDispatchersLifecycle() throws Exception
    {
        muleContext.start();

        //using sync endpoint so that any calls to 'process()' will be blocking and avoid timing issues
        OutboundEndpoint out = getTestOutboundEndpoint("out",
                                                       "test://out?exchangePattern=request-response", null, null, null, connector);

        // attempts to send/dispatch/request are made on a stopped/stopping connector
        // This should fail because the connector is not started!
        try
        {
            out.process(getTestEvent("data"));
            fail("cannot send on a connector that is not started");
        }
        catch (MessagingException e)
        {
            // expected
        }

        assertEquals(0, connector.dispatchers.getNumIdle());

        // Dispatcher is not started or connected
        assertDispatcherStartedConnected(out, false, false);

        connector.start();
        //This causes the first instance out dispatcher to be created
        assertDispatcherStartedConnected(out, true, true);

        OutboundEndpoint out2 = getTestOutboundEndpoint("out2",
                                                        "test://out2?exchangePattern=request-response", null, null, null, connector);
        //This causes the first instance out2 dispatcher to be created
        out2.process(getTestEvent("data"));

        //At this point there should be two idle, but the build server reports one, I suspect its a timing issues
        assertEquals(2, connector.dispatchers.getNumIdle());
        assertDispatcherStartedConnected(out, true, true);
        assertDispatcherStartedConnected(out2, true, true);

        connector.stop();

        // Pool is cleared because of implementation of workaround for MULE-4553
        assertEquals(0, connector.dispatchers.getNumActive() + connector.dispatchers.getNumIdle());
        assertDispatcherStartedConnected(out, false, false);
        assertDispatcherStartedConnected(out2, false, false);

        connector.start();

        assertEquals(2, connector.dispatchers.getNumActive() + connector.dispatchers.getNumIdle());
        assertDispatcherStartedConnected(out, true, true);
        assertDispatcherStartedConnected(out2, true, true);

        out.process(getTestEvent("data"));
        assertEquals(2, connector.dispatchers.getNumIdle());
        assertDispatcherStartedConnected(out, true, true);

        connector.dispose();
        assertEquals(0, connector.dispatchers.getNumActive() + connector.dispatchers.getNumIdle());

    }

    @Test
    public void testDispatcherFullLifecycle() throws Exception
    {
        OutboundEndpoint out = getTestOutboundEndpoint("out", "test://out", null, null, null, connector);

        MessageDispatcher dispatcher = connector.getDispatcherFactory().create(out);
        dispatcher.initialise();

        assertTrue(dispatcher.getLifecycleState().isInitialised());
        dispatcher.connect();
        assertTrue(dispatcher.isConnected());

        dispatcher.start();
        assertTrue(dispatcher.getLifecycleState().isStarted());

        dispatcher.stop();
        assertTrue(dispatcher.getLifecycleState().isStopped());

        dispatcher.disconnect();
        assertFalse(dispatcher.isConnected());

        dispatcher.dispose();
        assertTrue(dispatcher.getLifecycleState().isDisposed());

    }

    @Test
    public void testRequestersLifecycle() throws Exception
    {
        InboundEndpoint in = getTestInboundEndpoint("in", "test://in", null, null, null, connector);

        // attempts to send/dispatch/request are made on a stopped/stopping connector
        // This should fail because the connector is not started!
        try
        {
            in.request(1000L);
            fail("cannot sent on a connector that is not started");
        }
        catch (LifecycleException e)
        {
            //Expected
        }


        assertEquals(0, connector.requesters.getNumIdle());

        // Dispatcher is not started or connected
        assertRequesterStartedConnected(in, false, false);

        connector.start();
        assertRequesterStartedConnected(in, true, true);

        assertEquals(1, connector.requesters.getNumIdle());

        InboundEndpoint in2 = getTestInboundEndpoint("in2", "test://in2", null, null, null, connector);
        in2.request(1000L);


        assertEquals(2, connector.requesters.getNumIdle());
        assertRequesterStartedConnected(in, true, true);
        assertRequesterStartedConnected(in2, true, true);

        connector.stop();

        // Pool is cleared because of implementation of workaround for MULE-4553
        assertEquals(0, connector.requesters.getNumActive() + connector.requesters.getNumIdle());
        assertRequesterStartedConnected(in, false, false);
        assertRequesterStartedConnected(in2, false, false);

        connector.start();
        //Between Stop and start the requester pool maintains existing pooled objects
        assertEquals(2, connector.requesters.getNumActive() + connector.requesters.getNumIdle());
        assertRequesterStartedConnected(in, true, true);
        assertRequesterStartedConnected(in2, true, true);

        in.request(1000L);
        assertEquals(2, connector.requesters.getNumIdle());
        assertRequesterStartedConnected(in, true, true);

        connector.dispose();
        assertEquals(0, connector.requesters.getNumActive() + connector.requesters.getNumIdle());

    }

    @Test
    public void testRequesterFullLifecycle() throws Exception
    {
        InboundEndpoint in = getTestInboundEndpoint("out", "test://out", null, null, null, connector);

        MessageRequester requester = connector.getRequesterFactory().create(in);

        requester.initialise();
        assertTrue(requester.getLifecycleState().isInitialised());

        requester.connect();
        assertTrue(requester.isConnected());

        requester.start();
        assertTrue(requester.getLifecycleState().isStarted());

        requester.stop();
        assertTrue(requester.getLifecycleState().isStopped());

        requester.disconnect();
        assertFalse(requester.isConnected());

        requester.dispose();
        assertTrue(requester.getLifecycleState().isDisposed());

    }

    @Test
    public void testWorkManagerLifecycle() throws MuleException, WorkException
    {
        //ConnectorLifecycleTestCase These are now created in the "initialize" phase
        //   assertNull(connector.getReceiverWorkManager());
        //   assertNull(connector.getDispatcherWorkManager());
        //   assertNull(connector.getRequesterWorkManager());

        connector.start();
        assertNotNull(connector.getReceiverWorkManager());
        assertNotNull(connector.getDispatcherWorkManager());
        assertNotNull(connector.getRequesterWorkManager());
        connector.getReceiverWorkManager().doWork(createSomeWork());
        connector.getDispatcherWorkManager().doWork(createSomeWork());
        connector.getRequesterWorkManager().doWork(createSomeWork());

        connector.stop();
        assertNull(connector.getReceiverWorkManager());
        assertNull(connector.getDispatcherWorkManager());
        assertNull(connector.getRequesterWorkManager());

        connector.start();
        assertNotNull(connector.getReceiverWorkManager());
        assertNotNull(connector.getDispatcherWorkManager());
        assertNotNull(connector.getRequesterWorkManager());
        connector.getReceiverWorkManager().doWork(createSomeWork());
        connector.getDispatcherWorkManager().doWork(createSomeWork());
        connector.getRequesterWorkManager().doWork(createSomeWork());

        connector.dispose();
        assertNull(connector.getReceiverWorkManager());
        assertNull(connector.getDispatcherWorkManager());
        assertNull(connector.getRequesterWorkManager());
    }

    @Test
    public void testSchedulerLifecycle() throws MuleException, WorkException
    {
        assertNull(connector.getScheduler());

        connector.start();
        assertFalse(connector.getScheduler().isShutdown());
        assertFalse(connector.getScheduler().isTerminated());

        connector.stop();
        assertNull(connector.getScheduler());

        connector.start();
        assertFalse(connector.getScheduler().isShutdown());
        assertFalse(connector.getScheduler().isTerminated());

        connector.dispose();
        assertNull(connector.getScheduler());
    }

    protected Work createSomeWork()
    {
        return new Work()
        {
            @Override
            public void run()
            {
                System.out.println("I'm doing some work");
            }

            @Override
            public void release()
            {
                // nothing to do
            }
        };
    }

    private void assertDispatcherStartedConnected(OutboundEndpoint out, boolean started, boolean connected)
            throws Exception
    {
        AbstractMessageDispatcher dispatcher = (AbstractMessageDispatcher) connector.dispatchers.borrowObject(out);
        assertEquals("Dispatcher started", started, dispatcher.isStarted());
        assertEquals("Dispatcher connected", connected, dispatcher.isConnected());
        connector.dispatchers.returnObject(out, dispatcher);
    }

    private void assertRequesterStartedConnected(InboundEndpoint in, boolean started, boolean connected)
            throws Exception
    {
        AbstractMessageRequester requester = (AbstractMessageRequester) connector.requesters.borrowObject(in);
        assertEquals("Requester started", started, requester.isStarted());
        assertEquals("requester connected", connected, requester.isConnected());
        connector.requesters.returnObject(in, requester);
    }
}
