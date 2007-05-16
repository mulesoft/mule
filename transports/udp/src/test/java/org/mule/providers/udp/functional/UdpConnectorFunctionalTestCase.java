/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.udp.functional;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.HashSet;
import java.util.Set;

public class UdpConnectorFunctionalTestCase extends FunctionalTestCase
{

    public static final String MESSAGE = "hello";


    protected String getConfigResources()
    {
        return "udp-functional-test.xml";
    }

    public void testSendTestData() throws Exception
    {
        final int numberOfMessages = 2;
        MuleClient client = new MuleClient();

        for (int sentPackets = 0; sentPackets < numberOfMessages; sentPackets++)
        {
            String msg = MESSAGE + sentPackets;
            client.dispatch("serverEndpoint", msg, null);
        }

        Set receivedMessages = new HashSet(numberOfMessages);

        int receivedPackets = 0;
        for (; receivedPackets < numberOfMessages; receivedPackets++)
        {
            receivedMessages.add(client.receive("vm://foo", 60000).getPayloadAsString());

        }

        assertEquals(numberOfMessages, receivedPackets);

        for (int x = 0; numberOfMessages < receivedMessages.size(); x++)
        {
            String message = MESSAGE + x + " Received";
            assertTrue("checking for received message '" + message + "'", receivedMessages.contains(message));
        }
    }

}
