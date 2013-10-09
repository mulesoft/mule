/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import org.junit.Rule;
import org.junit.Test;

public class TCPTimeoutsTest extends FunctionalTestCase
{

    @Rule
    public DynamicPort tcpPort = new DynamicPort("tcpPort");

    @Override
    protected String getConfigResources()
    {
        return "tcp-response-timeout-config.xml";
    }

    @Test
    public void testOutboundResponseTimeoutSet() throws Exception
    {
        final MuleClient client = new DefaultLocalMuleClient(muleContext);

        final MuleMessage result = client.send("vm://testIn", TEST_MESSAGE, null);

        assertEquals(NullPayload.getInstance(), result.getPayload());
    }
}
