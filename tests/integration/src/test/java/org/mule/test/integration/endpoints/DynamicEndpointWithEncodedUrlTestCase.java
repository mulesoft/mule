/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.endpoints;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class DynamicEndpointWithEncodedUrlTestCase extends org.mule.tck.junit4.FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/endpoints/dynamic-endpoint-encoded-url-config.xml";
    }

    @Test
    public void encodesUrlInDynamicEndpoint() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testInput", TEST_MESSAGE, null);

        assertThat(response.getPayloadAsString(), equalTo("Processed"));
    }
}
