/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class TcpLengthFunctionalTestCase extends FunctionalTestCase
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    public TcpLengthFunctionalTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "tcp-length-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    public void testDispatchAndReplyViaStream() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        client.dispatch("asyncClientEndpoint1", TEST_MESSAGE, props);
        // MULE-2754
        Thread.sleep(200);
        UMOMessage result =  client.request("asyncClientEndpoint1", 3000);
        // expect failure - streaming not supported
        assertNull(result);
    }

    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        client.dispatch("asyncClientEndpoint2", TEST_MESSAGE, props);
        // MULE-2754
        Thread.sleep(200);
        UMOMessage result =  client.request("asyncClientEndpoint2", 3000);
        // expect failure - TCP simply can't work like this
        assertNull(result);
    }

}