/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.endpoints;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase.ConfigVariant;
import org.mule.tck.IntegrationDynamicPortTestCase;

public class DynamicEndpointWithAsyncResponseTestCase extends IntegrationDynamicPortTestCase
{

    public DynamicEndpointWithAsyncResponseTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/endpoints/dynamic-endpoint-with-async-response-config.xml";
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "org/mule/test/integration/endpoints/dynamic-endpoint-with-async-response-config.xml"}

        });
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

    @Test
    public void testDynamicEndpointWithAsyncResponse() throws Exception
    {

        DefaultMuleMessage message = new DefaultMuleMessage("hello", muleContext);
        message.setOutboundProperty("host", "localhost");
        message.setOutboundProperty("port", getPorts().get(0));
        message.setOutboundProperty("path", "/TEST");

        DefaultLocalMuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage response = client.send("vm://vmProxy", message);
        assertEquals("hello Received", response.getPayloadAsString());

        response = client.request("vm://vmOut", 5000);
        assertNotNull(response);
    }
}
