/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import org.mule.api.FutureMessageResult;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

// MULE-5162
// FIXME: refactor since it's a copy of DynamicEndpointRoutingTestCase with a jms outbound endpoint
public class DynamicJmsEndpointRoutingTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "dynamic-endpoint-routing-test-service.xml"},
            {ConfigVariant.FLOW, "dynamic-endpoint-routing-test-flow.xml"}});
    }

    public DynamicJmsEndpointRoutingTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testDynamicEndpoint() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        FutureMessageResult result = client.sendAsync("vm://inBound", "Hello,world", null, 5000);
        MuleMessage response = result.getMessage(5000);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals("step2Service", response.getPayloadAsString());
    }
}
