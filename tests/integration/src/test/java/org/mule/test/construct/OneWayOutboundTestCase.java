/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

