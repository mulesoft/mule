/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpPollingFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpPollingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "mule-http-polling-config-service.xml"},
            {ConfigVariant.FLOW, "mule-http-polling-config-flow.xml"}});
    }

    @Test
    public void testPollingHttpConnector() throws Exception
    {
        FunctionalTestComponent ftc = getFunctionalTestComponent("polled");
        assertNotNull(ftc);
        ftc.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                assertEquals("The Accept header should be set on the incoming message", "application/xml",
                    context.getMessage().<String> getInboundProperty("Accept"));
            }
        });

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request("vm://toclient", RECEIVE_TIMEOUT);
        assertNotNull(result.getPayload());
        assertEquals("foo", result.getPayloadAsString());
    }
}
