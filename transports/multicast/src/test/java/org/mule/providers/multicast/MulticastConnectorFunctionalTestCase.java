/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.multicast;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashSet;
import java.util.Set;

public class MulticastConnectorFunctionalTestCase extends FunctionalTestCase
{
    public static final String MESSAGE = "hello";


    protected String getConfigResources()
    {
        return "multicast-functional-test.xml";
    }

    public void testSendTestData() throws Exception
    {
        final int numberOfMessages = 2;
        MuleClient client = new MuleClient();

        logger.debug("sending messages");
        for (int sentPackets = 0; sentPackets < numberOfMessages; sentPackets++)
        {
            String msg = MESSAGE + sentPackets;
            client.dispatch("serverEndpoint", msg, null);
        }

        int broadcastMessages = numberOfMessages * 3; //3 components
        Set receivedMessages = new HashSet(broadcastMessages);

        logger.debug("receiving messages");
        int receivedPackets = 0;
        for (; receivedPackets < broadcastMessages; receivedPackets++)
        {
            UMOMessage message = client.request("vm://foo", 2000);
            assertNotNull(message);
            receivedMessages.add(message.getPayloadAsString());
        }

        assertEquals(broadcastMessages, receivedPackets);

        //Check all broadcasts were received from Component1
        checkBroadcastMessagesForComponent(numberOfMessages, receivedMessages, "Component1");

        //Check all broadcasts were received from Component2
        checkBroadcastMessagesForComponent(numberOfMessages, receivedMessages, "Component2");

        //Check all broadcasts were received from Component3
        checkBroadcastMessagesForComponent(numberOfMessages, receivedMessages, "Component3");

        assertEquals(0, receivedMessages.size());
    }

    protected void checkBroadcastMessagesForComponent(int numberOfMessages,
                                                      Set receivedMessages, String name)
    {
        //Check all broadcasts were received from Component2
        for (int x = 0; x < numberOfMessages; x++)
        {
            String expected = MESSAGE + x + " Received " + name;

            assertTrue(receivedMessages.contains(expected));
            assertTrue(receivedMessages.remove(expected));
        }
    }
    
}
