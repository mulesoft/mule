/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security;

import static org.junit.Assert.assertEquals;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * See MULE-3851
 */
public class OutboundHttpEndpointAuthenticationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/security/outbound-http-endpoint-authentication-test-flow.xml";
    }

    @Test
    public void testOutboundAutenticationSend() throws Exception
    {
        MuleClient client = muleContext.getClient();
        String payload = client.send("outbound", TEST_MESSAGE, null).getPayloadAsString();
        assertEquals(TEST_MESSAGE, payload);
    }

    @Test
    public void testOutboundAutenticationDispatch() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("outbound", TEST_MESSAGE, null);
        String payload = client.request("out", RECEIVE_TIMEOUT).getPayloadAsString();
        assertEquals(TEST_MESSAGE, payload);
    }
}
