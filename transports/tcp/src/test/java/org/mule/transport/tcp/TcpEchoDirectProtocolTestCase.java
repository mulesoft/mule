/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class TcpEchoDirectProtocolTestCase extends FunctionalTestCase 
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    protected String getConfigResources()
    {
        return "tcp-echo-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        String url = "tcp://localhost:61200";
        
        MuleMessage response = client.send(url, TEST_MESSAGE, null);
        assertNotNull(response);
        assertEquals(TEST_MESSAGE, response.getPayload());
    }

}
