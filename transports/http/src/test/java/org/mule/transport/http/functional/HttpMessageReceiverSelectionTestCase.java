/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpMessageReceiverSelectionTestCase extends FunctionalTestCase
{
    private static final String FLOW_A = "flowA";
    private static final String FLOW_B = "flowB";
    private static final String FLOW_C = "flowC";
    private static final String FLOW_D = "flowD";
    private static final String FLOW_X = "flowX";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("http.port");

    @Override
    protected String getConfigFile()
    {
        return "http-message-receiver-selection-config.xml";
    }

    @Test
    public void testHttpMessageReceiverSelectionTestCase() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send(getUrl(FLOW_A), TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(FLOW_A));
        response = client.send(getUrl(FLOW_A + "/" + FLOW_B), TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(FLOW_B));
        response = client.send(getUrl(FLOW_C), TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(FLOW_C));
        response = client.send(getUrl(FLOW_D), TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(FLOW_D));

        response = client.send(getUrl(FLOW_A + "/extra"), TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(FLOW_A));
        response = client.send(getUrl(FLOW_A + "/" + FLOW_B + "/extra"), TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(FLOW_B));

        response = client.send(getUrl("/extra"), TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(FLOW_X));
        response = client.send(getUrl(""), TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(FLOW_X));
    }

    private String getUrl(String path)
    {
        return String.format("http://localhost:%s/%s", dynamicPort.getNumber(), path);
    }

}
