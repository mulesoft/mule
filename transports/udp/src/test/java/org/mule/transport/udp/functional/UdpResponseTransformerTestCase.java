/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp.functional;

import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.udp.util.UdpClient;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class UdpResponseTransformerTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "udp-response-transformer-config.xml";
    }

    @Test
    @Ignore("MULE-6926: Flaky Test")
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
