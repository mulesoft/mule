/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.udp.functional;

import org.mule.tck.DynamicPortTestCase;
import org.mule.transport.udp.util.UdpClient;

public class UdpResponseTransformerTestCase extends DynamicPortTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "udp-response-transformer-config.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

    public void testResponseTransformer() throws Exception
    {
        UdpClient client = null;
        try
        {
            int port = getPorts().get(0).intValue();
            client = new UdpClient(port);
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
