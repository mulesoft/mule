/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.client;


import org.mule.RegistryContext;
import org.mule.extras.client.MuleClient;
import org.mule.extras.client.RemoteDispatcher;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class MuleClientRemotingTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/client/test-client-mule-config-remoting-tcp.xml";
    }

    public String getServerUrl()
    {
        return "tcp://localhost:60504";
    }

    // public void testClientSendToRemoteComponent() throws Exception
    // {
    // // Will connect to the server using tcp://localhost:60504
    // MuleClient client = new MuleClient();
    // RegistryContext.getConfiguration().setSynchronous(true);
    //
    // RemoteDispatcher dispatcher = client.getRemoteDispatcher(getServerUrl());
    // UMOMessage message = dispatcher.sendToRemoteComponent("TestReceiverUMO", "Test
    // Client Send message", null);
    // assertNotNull(message);
    // assertEquals("Received: Test Client Send message", message.getPayload());
    // }
    //
    //    RemoteDispatcher dispatcher = client.getRemoteDispatcher(getServerUrl());
    //    UMOMessage message = dispatcher.sendToRemoteComponent("TestReceiverUMO", "Test Client Send message",
    //        null);
    //    assertNotNull(message);
    //    assertEquals("Test Client Send message Received", message.getPayload());
    //}

    public void testClientRequestResponseOnEndpoint() throws Exception
    {
        // Will connect to the server using tcp://localhost:60504
        MuleClient client = new MuleClient();
        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);

        RemoteDispatcher dispatcher = client.getRemoteDispatcher(getServerUrl());
        UMOMessage message = dispatcher.sendRemote("vm://remote.endpoint?connector=vmRemoteConnector", "foo",
            null);
        assertNotNull(message);
        assertEquals("received from remote", message.getPayloadAsString());
    }

    // public void testClientSendAndReceiveRemote() throws Exception
    // {
    // String remoteEndpoint = "vm://remote.queue";
    // // Will connect to the server using tcp://localhost:60504
    // MuleClient client = new MuleClient();
    // RegistryContext.getConfiguration().setSynchronous(true);
    //
    // RemoteDispatcher dispatcher = client.getRemoteDispatcher(getServerUrl());
    // UMOMessage message = dispatcher.receiveRemote(remoteEndpoint, 1000);
    // assertNull(message);
    //
    // // Dispatch a message
    // dispatcher.dispatchRemote(remoteEndpoint, "Test Remote Message 2", null);
    //
    // // Receive the message
    // message = dispatcher.receiveRemote(remoteEndpoint, 2000);
    // assertNotNull(message);
    // assertEquals("Test Remote Message 2", message.getPayload());
    //
    // }
}
