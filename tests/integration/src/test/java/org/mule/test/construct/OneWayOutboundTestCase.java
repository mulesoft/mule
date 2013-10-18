/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

public abstract class OneWayOutboundTestCase extends FunctionalTestCase
{

    private LocalMuleClient client;
    
    @Before
    public void setUp() throws Exception
    {
        client = muleContext.getClient();
    }

    @Test
    public void noOutbound() throws Exception
    {
        MuleMessage response = client.send("vm://noOutbound", "TEST", null);
        assertEquals("TEST processed", response.getPayload());
    }

    @Test
    public void noOutboundEndpointAsync() throws Exception
    {
        MuleMessage response = client.send("vm://noOutboundAsync", "TEST", null);
        assertEquals("TEST", response.getPayload());
    }

    @Test
    public void oneWayOutbound() throws Exception
    {
        MuleMessage response = client.send("vm://oneWayOutbound", "TEST", null);
        assertOneWayOutboundResponse(response);
    }

    protected abstract void assertOneWayOutboundResponse(MuleMessage response);

    @Test
    public void oneWayOutboundAfterComponent() throws Exception
    {
        MuleMessage response = client.send("vm://oneWayOutboundAfterComponent", "TEST", null);
        assertOneWayOutboundAfterComponentResponse(response);
    }

    protected  abstract void assertOneWayOutboundAfterComponentResponse(MuleMessage response);

    @Test
    public void oneWayOutboundBeforeComponent() throws Exception
    {
        MuleMessage response = client.send("vm://oneWayOutboundBeforeComponent", "TEST", null);
        assertEquals("TEST processed", response.getPayload());
    }
}

