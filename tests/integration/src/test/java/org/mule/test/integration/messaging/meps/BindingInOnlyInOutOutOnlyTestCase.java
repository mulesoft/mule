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

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

//START SNIPPET: full-class
public class BindingInOnlyInOutOutOnlyTestCase extends AbstractServiceAndFlowTestCase
{
    public static final long TIMEOUT = 3000;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/messaging/meps/pattern_binding-In-Only_In-Out_Out-Only-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/messaging/meps/pattern_binding-In-Only_In-Out_Out-Only-flow.xml"}});
    }

    public BindingInOnlyInOutOutOnlyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("inboundEndpoint", new int[]{1, 2, 3, 4, 5}, null);

        MuleMessage result = client.request("receivedEndpoint", TIMEOUT);
        assertNotNull(result);
        assertEquals("Total: 15", result.getPayloadAsString());
    }
}
