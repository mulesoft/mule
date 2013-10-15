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
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DynamicEndpointRoutingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "dynamic-endpoint-routing-test.xml";
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
