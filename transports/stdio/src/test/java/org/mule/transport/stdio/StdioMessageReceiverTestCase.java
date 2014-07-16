/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.stdio;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.AbstractMessageReceiverTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StdioMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    @Test
    public void testReceiver() throws Exception
    {
        // FIX A bit hard testing receive from a unit test as we need to reg
        // listener etc
        // file endpoint functions tests for this
    }

    @Test
    public void testOtherProperties() throws Exception
    {
        StdioMessageReceiver receiver = (StdioMessageReceiver) getMessageReceiver();

        Service service = getTestService("orange", Orange.class);
        assertNotNull(service);

        endpoint.getConnector().start();

        receiver.setFrequency(1001);
        receiver.setInputStream(System.in);

        assertTrue(receiver.getFrequency() == 1001);
        receiver.setFrequency(0);
        assertTrue(receiver.getFrequency() == StdioMessageReceiver.DEFAULT_POLL_FREQUENCY);
    }

    public MessageReceiver getMessageReceiver() throws CreateException
    {
        return new StdioMessageReceiver(endpoint.getConnector(), service, endpoint, 1000);
    }

    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getEndpointFactory().getInboundEndpoint("stdio://System");
    }
}
