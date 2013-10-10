/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InOnlyOutOnlyTestCase extends FunctionalTestCase
{

    public static final long TIMEOUT = 3000;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Only_Out-Only.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        client.dispatch("inboundEndpoint", "some data", null);
        Map props = new HashMap();
        props.put("foo", "bar");
        client.dispatch("inboundEndpoint", "some data", props);

        MuleMessage result = client.request("receivedEndpoint", TIMEOUT);
        assertNotNull(result);
        assertEquals("foo header received", result.getPayloadAsString());

        result = client.request("notReceivedEndpoint", TIMEOUT);
        assertNotNull(result);
        assertEquals("foo header not received", result.getPayloadAsString());
    }
}
