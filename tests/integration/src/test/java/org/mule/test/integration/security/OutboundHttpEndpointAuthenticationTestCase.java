/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.security;

import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

/**
 * See MULE-3851
 */
public class OutboundHttpEndpointAuthenticationTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/security/outbound-http-endpoint-authentication-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/security/outbound-http-endpoint-authentication-test-flow.xml"}
        });
    }

    public OutboundHttpEndpointAuthenticationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testOutboundAutenticationSend() throws Exception
    {
        MuleClient mc = new MuleClient(muleContext);
        assertEquals(TEST_MESSAGE, mc.send("outbound", TEST_MESSAGE, null).getPayloadAsString());
    }

    @Test
    public void testOutboundAutenticationDispatch() throws Exception
    {
        MuleClient mc = new MuleClient(muleContext);
        mc.dispatch("outbound", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, mc.request("out", RECEIVE_TIMEOUT).getPayloadAsString());
    }
}
