/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class AsynchMule1869TestCase extends FunctionalTestCase
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    public AsynchMule1869TestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "tcp-functional-test.xml";
    }

    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        client.dispatch("asyncClientEndpoint", TEST_MESSAGE, props);
        Thread.sleep(2000);
        logger.debug("----------------- receive now");
        UMOMessage result =  client.request("asyncClientEndpoint", 10000);
        assertNotNull("No message received", result);
        assertEquals(TEST_MESSAGE + " Received Async", result.getPayloadAsString());
    }

    public void testDispatchAndReplyWithReceive() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        client.dispatch("asyncClientEndpoint", TEST_MESSAGE, props);
        Thread.sleep(2000);
        logger.debug("----------------- receive now");
        UMOMessage result =  client.receive("asyncClientEndpoint", 10000);
        assertNotNull("No message received", result);
        assertEquals(TEST_MESSAGE + " Received Async", result.getPayloadAsString());
    }

}