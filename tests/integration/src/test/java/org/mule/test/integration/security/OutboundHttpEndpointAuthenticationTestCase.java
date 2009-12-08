/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.security;

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

/**
 * See MULE-3851
 */
public class OutboundHttpEndpointAuthenticationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/security/outbound-http-endpoint-authentication-test.xml";
    }

    public void testOutboundAutenticationSend() throws Exception
    {
        MuleClient mc = new MuleClient();
        assertEquals(TEST_MESSAGE, mc.send("outbound", TEST_MESSAGE, null).getPayloadAsString());
    }

    public void testOutboundAutenticationDispatch() throws Exception
    {
        MuleClient mc = new MuleClient();
        mc.dispatch("outbound", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, mc.request("out", RECEIVE_TIMEOUT).getPayloadAsString());
    }
}
