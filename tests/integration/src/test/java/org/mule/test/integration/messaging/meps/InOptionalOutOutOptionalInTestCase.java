/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

@Ignore("These tests have a property propagation / MEPs issue")
public class InOptionalOutOutOptionalInTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Optional-In.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send("inboundEndpoint", "some data", null);
        assertNotNull(result);
        assertEquals(NullPayload.getInstance(), result.getPayload());
        //TODO Even though the component returns a null the remoteSync is honoured.
        // I don't think this is right for Out-Optional-In, but probably should be the behaviour for Out-In
        assertEquals("Received", result.getInboundProperty("externalApp"));

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "bar");
        result = client.send("inboundEndpoint", "some data", props);
        assertNotNull(result);
        assertEquals("bar header received", result.getPayload());
        assertEquals("Received", result.getInboundProperty("externalApp"));
    }
}
