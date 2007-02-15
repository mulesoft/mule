/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.http;

import org.mule.extras.client.MuleClient;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.http.HttpConnector;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    protected static final String TEST_MESSAGE = "Test Http Request";

    private List results = new ArrayList();

    protected UMOEndpointURI getInDest()
    {
        try
        {
            return new MuleEndpointURI("http://localhost:60198");
        }
        catch (EndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        return null;
    }

    protected UMOConnector createConnector() throws Exception
    {
        HttpConnector connector = new HttpConnector();
        connector.setName("testHttp");
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        return connector;
    }

    protected void sendTestData(int iterations) throws Exception
    {
        MuleClient client = new MuleClient();
        for (int i = 0; i < iterations; i++)
        {
            UMOMessage m = client.send(getInDest().toString(), TEST_MESSAGE + i, null);
            assertNotNull(m);
            results.add(m.getPayload());
        }
    }

    protected void receiveAndTestResults() throws Exception
    {
        int i = 0;
        for (Iterator iterator = results.iterator(); iterator.hasNext(); i++)
        {
            byte[] result = (byte[])iterator.next();
            assertNotNull(result);
            assertEquals(TEST_MESSAGE + i + " Received", new String(result));
        }
    }

    protected String getClientUrl()
    {
        return "http://www.google.com.au";
    }

    public void testClient() throws Exception
    {
        if (isOffline("org.mule.test.integration.providers.http.HttpFunctionalTestCase.testClient()"))
        {
            return;
        }

        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("http.method", "GET");
        UMOMessage m = client.send(getClientUrl(), null, props);
        assertNotNull(m);
        assertNotNull(m.getPayload());
        logger.debug(m.getPayloadAsString());
        assertTrue(m.getPayloadAsString().indexOf("google") > -1);
    }

    public void testClientWithPath() throws Exception
    {
        if (isOffline("org.mule.test.integration.providers.http.HttpFunctionalTestCase.testClientWithPath()"))
        {
            return;
        }

        String requestAddress = "http://www.google.co.uk/intl/en/about.html";
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("http.method", "GET");

        UMOMessage m = client.send(requestAddress, null, props);
        assertNotNull(m);
        assertNotNull(m.getPayload());
        assertTrue(m.getPayloadAsString().indexOf("About Google") > -1);
    }
}
