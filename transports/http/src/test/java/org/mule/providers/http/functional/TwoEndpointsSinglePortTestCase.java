/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.functional;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import java.util.ArrayList;
import java.util.List;

public class TwoEndpointsSinglePortTestCase extends FunctionalTestCase
{
    public TwoEndpointsSinglePortTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "two-endpoints-single-port.xml";
    }

    public void testSendToEach() throws Exception
    {

        sendWithResponse("http://localhost:60211/mycomponent1", "test", "mycomponent1", 10);
        sendWithResponse("http://localhost:60211/mycomponent2", "test", "mycomponent2", 10);
    }

    public void testSendToEachWithBadEndpoint() throws Exception
    {

        MuleClient client = new MuleClient();

        sendWithResponse("http://localhost:60211/mycomponent1", "test", "mycomponent1", 5);
        sendWithResponse("http://localhost:60211/mycomponent2", "test", "mycomponent2", 5);

        UMOMessage result = client.send("http://localhost:60211/mycomponent-notfound", "test", null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(404, result.getIntProperty("http.status", 0));

        // Test that after the exception the endpoints still receive events
        sendWithResponse("http://localhost:60211/mycomponent1", "test", "mycomponent1", 5);
        sendWithResponse("http://localhost:60211/mycomponent2", "test", "mycomponent2", 5);
    }

    protected void sendWithResponse(String endpoint, String message, String response, int noOfMessages)
        throws UMOException
    {
        MuleClient client = new MuleClient();

        List results = new ArrayList();
        for (int i = 0; i < noOfMessages; i++)
        {
            results.add(client.send(endpoint, message, null).getPayload(byte[].class));
        }

        assertEquals(noOfMessages, results.size());
        for (int i = 0; i < noOfMessages; i++)
        {
            assertEquals(response, new String((byte[])results.get(i)));
        }
    }
}
