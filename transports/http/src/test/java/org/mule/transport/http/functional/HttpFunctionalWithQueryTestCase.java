/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpFunctionalWithQueryTestCase extends FunctionalTestCase
{
    
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    
    @Override
    protected String getConfigResources()
    {
        return "http-functional-test-with-query.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("clientEndpoint1", null, null);
        assertEquals("boobar", result.getPayloadAsString());
    }

    @Test
    public void testSendWithParams() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "noo");
        props.put("far", "nar");
        MuleMessage result = client.send("clientEndpoint2", null, props);
        assertEquals("noonar", result.getPayloadAsString());
    }

    @Test
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
        catch (DispatchException e)
        {
            //expected
            assertTrue(e.getCause() instanceof RequiredValueException);
        }
    }
}
