/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.client;


import org.mule.RegistryContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.tck.FunctionalTestCase;

public abstract class AbstractClientRemotingTestCase extends FunctionalTestCase
{

    public abstract String getRemoteEndpointUri();

    public void testClientSendToRemoteComponent() throws Exception
    {
        // Will connect to the server using remote endpoint
        MuleClient client = new MuleClient();
        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);

        RemoteDispatcher dispatcher = client.getRemoteDispatcher(getRemoteEndpointUri());
        MuleMessage message = dispatcher.sendToRemoteComponent("TestReceiverUMO", "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Test Client Send message Received", message.getPayload());
    }

    public void testClientRequestResponseOnEndpoint() throws Exception
    {
        // Will connect to the server using tcp://localhost:60504
        MuleClient client = new MuleClient();
        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);

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
    public void testClientSendAndReceiveRemote() throws Exception
    {
        String remoteEndpoint = "vm://remote.queue?connector=vmRemoteQueueConnector";
        // Will connect to the server using The Server endpoint
        MuleClient client = new MuleClient();
        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);

        RemoteDispatcher dispatcher = client.getRemoteDispatcher(getRemoteEndpointUri());
        MuleMessage message = dispatcher.receiveRemote(remoteEndpoint, RECEIVE_TIMEOUT);
        assertNull(message);
        // We do a send instead of a dispatch here so the operation is
        // synchronous thus eaiser to test
        dispatcher.sendRemote(remoteEndpoint, "Test Remote Message 2", null);

        message = dispatcher.receiveRemote(remoteEndpoint, RECEIVE_TIMEOUT);
        assertNotNull(message);
        assertEquals("Test Remote Message 2", message.getPayload());
    }
}