/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Before;
import org.junit.Test;

public abstract class OneWayOutboundTestCase extends AbstractIntegrationTestCase
{

    private MuleClient client;
    
    @Before
    public void setUp() throws Exception
    {
        client = muleContext.getClient();
    }

    @Test
    public void noOutbound() throws Exception
    {
        MuleMessage response = flowRunner("noOutbound").withPayload("TEST").run().getMessage();
        assertEquals("TEST processed", response.getPayload());
    }

    @Test
    public void noOutboundEndpointAsync() throws Exception
    {
        MuleMessage response = flowRunner("noOutboundAsync").withPayload("TEST").run().getMessage();
        assertEquals("TEST", response.getPayload());
    }

    @Test
    public void oneWayOutbound() throws Exception
    {
        MuleMessage response = flowRunner("oneWayOutbound").withPayload("TEST").run().getMessage();
        assertOneWayOutboundResponse(response);
    }

    protected abstract void assertOneWayOutboundResponse(MuleMessage response);

    @Test
    public void oneWayOutboundAfterComponent() throws Exception
    {
        MuleMessage response = flowRunner("oneWayOutboundAfterComponent").withPayload("TEST").run().getMessage();
        assertOneWayOutboundAfterComponentResponse(response);
    }

    protected  abstract void assertOneWayOutboundAfterComponentResponse(MuleMessage response);

    @Test
    public void oneWayOutboundBeforeComponent() throws Exception
    {
        MuleMessage response = flowRunner("oneWayOutboundBeforeComponent").withPayload("TEST").run().getMessage();
        assertEquals("TEST processed", response.getPayload());
    }
}

