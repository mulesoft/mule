/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;


import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public abstract class AbstractClientRemotingTestCase extends FunctionalTestCase
{

    public abstract String getRemoteEndpointUri();

    @Test
    public void testClientSendToRemoteComponent() throws Exception
    {
        // Will connect to the server using remote endpoint
        MuleClient client = new MuleClient(muleContext);
 
        RemoteDispatcher dispatcher = client.getRemoteDispatcher(getRemoteEndpointUri());
        MuleMessage message = dispatcher.sendToRemoteComponent("TestReceiverUMO", "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Test Client Send message Received", message.getPayload());
    }

    @Test
    public void testClientRequestResponseOnEndpoint() throws Exception
    {
        // Will connect to the server using tcp://localhost:60504
        MuleClient client = new MuleClient(muleContext);
 
        RemoteDispatcher dispatcher = client.getRemoteDispatcher(getRemoteEndpointUri());
        MuleMessage message = dispatcher.sendRemote("vm://remote.endpoint?connector=vmRemoteConnector", "foo",
                null);
        assertNotNull(message);
        assertEquals("received from remote component", message.getPayloadAsString());
    }

    /**
     * A test that writes a message to a remote queue
     *
     * @throws Exception
     */
    @Test
    public void testClientSendAndReceiveRemote() throws Exception
    {
        String remoteEndpoint = "vm://remote.queue?connector=vmRemoteQueueConnector";
        // Will connect to the server using The Server endpoint
        MuleClient client = new MuleClient(muleContext);

        RemoteDispatcher dispatcher = client.getRemoteDispatcher(getRemoteEndpointUri());
        // Doubling timeout see MULE-3000
        // Use TIMEOUT_NOT_SET_VALUE as we need respective remoteDispatcherEndpoint to you timeout as defined on the endpoint.  
        MuleMessage message = dispatcher.receiveRemote(remoteEndpoint,MuleEvent.TIMEOUT_NOT_SET_VALUE);
        assertNull(message);
        // We do a send instead of a dispatch here so the operation is
        // synchronous thus eaiser to test
        dispatcher.sendRemote(remoteEndpoint, "Test Remote Message 2", null);

        // Doubling timeout see MULE-3000
        message = dispatcher.receiveRemote(remoteEndpoint, RECEIVE_TIMEOUT * 2);
        assertNotNull(message);
        assertEquals("Test Remote Message 2", message.getPayload());
    }
    
}
