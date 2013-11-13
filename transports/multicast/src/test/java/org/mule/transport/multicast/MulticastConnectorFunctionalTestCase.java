/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.multicast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class MulticastConnectorFunctionalTestCase extends FunctionalTestCase
{
    public static final String MESSAGE = "hello";

    @Override
    protected String getConfigFile()
    {
        return "multicast-functional-test.xml";
    }

    @Test
    public void testSendTestData() throws Exception
    {
        final int numberOfMessages = 2;
        MuleClient client = muleContext.getClient();

        logger.debug("sending messages");
        for (int sentPackets = 0; sentPackets < numberOfMessages; sentPackets++)
        {
            String msg = MESSAGE + sentPackets;
            client.dispatch("serverEndpoint", msg, null);
        }

        int broadcastMessages = numberOfMessages * 3; //3 components
        Set<String> receivedMessages = new HashSet<String>(broadcastMessages);

        logger.debug("receiving messages");
        int receivedPackets = 0;
        for (; receivedPackets < broadcastMessages; receivedPackets++)
        {
            MuleMessage message = client.request("vm://foo", 2000);
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
                                                      Set<String> receivedMessages, String componentName)
    {
        //Check all broadcasts were received from component <name>
        for (int x = 0; x < numberOfMessages; x++)
        {
            String expected = MESSAGE + x +  componentName;

            assertTrue(receivedMessages.contains(expected));
            assertTrue(receivedMessages.remove(expected));
        }
    }

}
