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
import org.mule.extras.client.MuleClient;
import org.mule.extras.client.RemoteDispatcher;
import org.mule.tck.FunctionalTestCase;

public class MuleClientRemotingJmsTestCase extends FunctionalTestCase
{

    public static int WAIT_TO_RECEIVE = 1000;
    public static int WAIT_TO_RESTART = 3000;

    public MuleClientRemotingJmsTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/client/test-client-mule-config-remote-jms.xml";
    }

    public void testClientSendToRemoteComponent() throws Exception
    {
        // Will connect to the server using jms://jmsSysProvider/mule.sys.queue
        MuleClient client = new MuleClient();
        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);

        RemoteDispatcher dispatcher = client.getRemoteDispatcher(getServerUrl());
        MuleMessage message =
                dispatcher.sendToRemoteComponent("TestReceiverUMO", "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Received: Test Client Send message", message.getPayload());
    }

    public void testClientDispatchAndReceiveRemote() throws Exception
    {
        // without this we get intermittent failures "sending to a deleted queue", which seems to
        // be the previous server.
        Thread.sleep(WAIT_TO_RESTART);
        
        String remoteEndpoint = "vm://vmRemoteProvider/remote.queue";
        // Will connect to the server using jms://jmsSysProvider/mule.sys.queue
        MuleClient client = new MuleClient();
        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);

        RemoteDispatcher dispatcher = client.getRemoteDispatcher(getServerUrl());
        MuleMessage message = dispatcher.receiveRemote(remoteEndpoint, WAIT_TO_RECEIVE);
        assertNull(message);

        // this used to be sendRemote, with the return value discarded and a comment
        // saying it made the test easier.  afaict that must have been some other attempt
        // to work around the problem that WAIT_TO_RESTART addresses?
        dispatcher.dispatchRemote(remoteEndpoint, "Test Remote Message 2", null);

        message = dispatcher.receiveRemote(remoteEndpoint, WAIT_TO_RECEIVE);
        assertNotNull(message);
        assertEquals("Test Remote Message 2", message.getPayload());
    }

    public String getServerUrl()
    {
        return "jms://jmsSysProvider/mule.sys.queue";
    }

}
