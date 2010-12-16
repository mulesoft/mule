/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;

import java.util.HashMap;
import java.util.Map;

public class HttpDynamicFunctionalTestCase extends DynamicPortTestCase
{
    protected static String TEST_REQUEST = "Test Http Request";

    @Override
    protected String getConfigResources()
    {
        return "http-dynamic-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("port", getPorts().get(0));
        props.put("path", "foo");

        MuleMessage result = client.send("clientEndpoint", TEST_REQUEST, props);
        assertEquals(TEST_REQUEST + " Received 1", result.getPayloadAsString());

        props.put("port", getPorts().get(1));
        result = client.send("clientEndpoint", TEST_REQUEST, props);
        assertEquals(TEST_REQUEST + " Received 2", result.getPayloadAsString());
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }
}
