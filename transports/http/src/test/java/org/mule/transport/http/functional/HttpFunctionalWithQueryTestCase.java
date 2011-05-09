/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;

import java.util.HashMap;
import java.util.Map;

public class HttpFunctionalWithQueryTestCase extends DynamicPortTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "http-functional-test-with-query.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("clientEndpoint1", null, null);
        assertEquals("boobar", result.getPayloadAsString());
    }

    public void testSendWithParams() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "noo");
        props.put("far", "nar");
        MuleMessage result = client.send("clientEndpoint2", null, props);
        assertEquals("noonar", result.getPayloadAsString());
    }

    public void testSendWithBadParams() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("hoo", "noo");
        props.put("har", "nar");

        try
        {
            client.send("clientEndpoint2", null, props);
            fail("Required values missing");
        }
        catch (Exception e)
        {
            assertTrue(e.getCause() instanceof RequiredValueException);
        }
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}
