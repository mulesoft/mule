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

import org.mule.api.transport.MessageReceiver;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.AbstractConnector;

import org.junit.Test;

public class PatternFlowStateTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/components/pattern-flows-initial-state.xml";
    }

    @Test
    public void testDefaultInitialstate() throws Exception
    {
        doTestStarted("defaultSimpleService", "in.simple.default");
    }

    @Test
    public void testStartedInitialstate() throws Exception
    {
        doTestStarted("startedSimpleService", "in.simple.started");
    }

    @Test
    public void testInitialStateStopped() throws Exception
    {
        doTestStopped("stoppedSimpleService", "in.simple.stopped");
    }

    @Test
    public void testBridgeDefaultInitialstate() throws Exception
    {
        doTestStarted("defaultBridge", "in.bridge.default");
    }

    @Test
    public void testBridgeStartedInitialstate() throws Exception
    {
        doTestStarted("startedBridge", "in.bridge.started");
    }

    @Test
    public void testBridgeInitialStateStopped() throws Exception
    {
        doTestStopped("stoppedBridge", "in.bridge.stopped");
    }

    @Test
    public void testValidatorDefaultInitialstate() throws Exception
    {
        doTestStarted("defaultValidator", "in.validator.default");
    }

    @Test
    public void testValidatorStartedInitialstate() throws Exception
    {
        doTestStarted("startedValidator", "in.validator.started");
    }

    @Test
    public void testValidatorInitialStateStopped() throws Exception
    {
        doTestStopped("stoppedValidator", "in.validator.stopped");
    }

    protected void doTestStarted(String flowName, String endpointName) throws Exception
    {
        AbstractFlowConstruct f = (AbstractFlowConstruct) muleContext.getRegistry().lookupFlowConstruct(
            flowName);
        // Flow initially started
        assertTrue(f.isStarted());
        assertFalse(f.isStopped());

        // The listeners should be registered and started.
        doListenerTests(endpointName, 1, true);
    }

    public void doTestStopped(String flowName, String endpointName) throws Exception
    {
        AbstractFlowConstruct f = (AbstractFlowConstruct) muleContext.getRegistry().lookupFlowConstruct(
            flowName);
        assertEquals("stopped", f.getInitialState());
        // Flow initially stopped
        assertFalse(f.isStarted());
        assertTrue(f.isStopped());

        // The connector should be started, but with no listeners registered.
        doListenerTests(endpointName, 0, true);

        f.start();
        assertTrue(f.isStarted());
        assertFalse(f.isStopped());

        // The listeners should now be registered and started.
        doListenerTests(endpointName, 1, true);
    }

    protected void doListenerTests(String receiverName, int expectedCount, boolean isConnected)
    {
        AbstractConnector connector = (AbstractConnector) muleContext.getRegistry().lookupConnector(
            "connector.test.mule.default");
        assertNotNull(connector);
        assertTrue(connector.isStarted());
        MessageReceiver[] receivers = connector.getReceivers("*" + receiverName + "*");
        assertEquals(expectedCount, receivers.length);
        for (int i = 0; i < expectedCount; i++)
        {
            assertEquals(isConnected, receivers[0].isConnected());
        }
    }

}
