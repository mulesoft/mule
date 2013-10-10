/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.messaging.meps;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

//START SNIPPET: full-class
public class InOnlyOutOnlyTestCase extends AbstractServiceAndFlowTestCase
{
    public static final long TIMEOUT = 3000;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/messaging/meps/pattern_In-Only_Out-Only-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/messaging/meps/pattern_In-Only_Out-Only-flow.xml"}
        });
    }

    public InOnlyOutOnlyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("inboundEndpoint", "some data", null);
        Map<String, Object> props = new HashMap<String, Object>();
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
