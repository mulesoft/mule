/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
import static org.junit.Assert.assertNull;

public class InOnlyOptionalOutTestCase extends AbstractServiceAndFlowTestCase
{
    public static final long TIMEOUT = 3000;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/messaging/meps/pattern_In-Only_Optional-Out-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/messaging/meps/pattern_In-Only_Optional-Out-flow.xml"}});
    }

    public InOnlyOptionalOutTestCase(ConfigVariant variant, String configResources)
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
        assertNull(result);
    }
}
