/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.extras.client.MuleClient;

import java.util.Map;
import java.util.HashMap;

public class TcpFunctionalTestCase extends FunctionalTestCase 
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    public TcpFunctionalTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "tcp-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        client.dispatch("asyncClientEndpoint", TEST_MESSAGE, props);

        UMOMessage result =  client.receive("asyncClientEndpoint", 4000);
        assertNotNull(result);
        assertEquals(TEST_MESSAGE + " Received Async", result.getPayloadAsString());
    }

}
