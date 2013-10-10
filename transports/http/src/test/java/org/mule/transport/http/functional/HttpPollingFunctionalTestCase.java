/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;

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
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                assertEquals("The Accept header should be set on the incoming message", "application/xml",
                    context.getMessage().<String> getInboundProperty("Accept"));
            }
        });
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request("vm://toclient", RECEIVE_TIMEOUT);
        assertNotNull(result.getPayload());
        assertEquals("foo", result.getPayloadAsString());
    }

}
