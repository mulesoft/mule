/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class HttpPostWithMapPayloadTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "http-post-with-map-config.xml";
    }

    @Test
    public void sendAndReceivesSameMapPayload() throws Exception
    {
        Map<String, String> mapPayload = new HashMap<String, String>();
        mapPayload.put("key1", "value1");
        mapPayload.put("key2", "value2");

        LocalMuleClient client = muleContext.getClient();
        client.dispatch("vm://testInput", mapPayload, null);

        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);
        assertEquals(mapPayload, response.getPayload());
    }

}
