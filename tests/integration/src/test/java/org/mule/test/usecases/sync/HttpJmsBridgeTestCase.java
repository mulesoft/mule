/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class HttpJmsBridgeTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/sync/http-jms-bridge-flow.xml";
    }

    @Test
    public void testBridge() throws Exception
    {
        MuleClient client = muleContext.getClient();
        String payload = "payload";

        Map<String, Object> headers = new HashMap<String, Object>();
        final String customHeader = "X-Custom-Header";
        headers.put(customHeader, "value");

        client.dispatch(String.format("http://localhost:%d/in", httpPort.getNumber()), new DefaultMuleMessage(payload, headers, muleContext), newOptions().method(POST.name()).build());

        MuleMessage msg = client.request("test://out", RECEIVE_TIMEOUT);
        assertNotNull(msg);
        assertThat(getPayloadAsString(msg), is(payload));
        assertThat(msg.getInboundProperty(customHeader), is("value"));
    }
}
