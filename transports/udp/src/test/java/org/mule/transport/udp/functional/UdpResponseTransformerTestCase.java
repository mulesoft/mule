/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp.functional;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.udp.util.UdpClient;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UdpResponseTransformerTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "udp-response-transformer-config.xml";
    }

    @Test
    public void testResponseTransformer() throws Exception
    {
        UdpClient client = null;
        try
        {
            client = new UdpClient(dynamicPort.getNumber());
            byte[] response = client.send(TEST_MESSAGE);

            String expected = TEST_MESSAGE + " In Out Out2";
            String result = new String(response).trim();
            assertEquals(expected, result);
        }
        finally
        {
            if (client != null)
            {
                client.shutdown();
            }
        }
    }
}
