/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class InOptionalOutOutOnlyTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only-flow.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send("inboundEndpoint", "some data", null);
        assertNotNull(result);
        assertEquals(NullPayload.getInstance(), result.getPayload());

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "bar");
        result = client.send("inboundEndpoint", "some data", props);
        assertNotNull(result);
        assertEquals("foo header received", result.getPayload());
    }
}
