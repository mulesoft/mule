/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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
