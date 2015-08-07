/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import org.junit.Rule;
import org.junit.Test;

public class JsonSchemaValidationFilterTestCase extends FunctionalTestCase
{

    private static final String JSON_ACCEPT =
            "{\n" +
            "  \"homeTeam\": \"BAR\",\n" +
            "  \"awayTeam\": \"RMA\",\n" +
            "  \"homeTeamScore\": 3,\n" +
            "  \"awayTeamScore\": 0\n" +
            "}";

    private static final String JSON_REJECT =
            "{\n" +
            "  \"homeTeam\": \"BARCA\",\n" +
            "  \"awayTeam\": \"RMA\",\n" +
            "  \"homeTeamScore\": 3,\n" +
            "  \"awayTeamScore\": 0\n" +
            "}";

    private static final String JSON_BROKEN =
            "{\n" +
            "  \"homeTeam\": BARCA\n" +
            "}";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "json-filter-config.xml";
    }

    @Test
    public void validSchema() throws Exception
    {
        MuleClient client = muleContext.getClient();
        final HttpRequestOptions httpRequestOptions = HttpRequestOptionsBuilder.newOptions().method(POST.name()).build();
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber(), getTestMuleMessage(JSON_ACCEPT), httpRequestOptions);
        assertEquals(200, message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals("accepted", message.getPayloadAsString());
    }

    @Test
    public void invalidSchema() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber(), getTestMuleMessage(JSON_REJECT));
        assertEquals(200, message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertFalse("accepted".equals(message.getPayloadAsString()));
    }

    @Test
    public void brokenJson() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber(), getTestMuleMessage(JSON_BROKEN));
        assertEquals(200, message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertFalse("accepted".equals(message.getPayloadAsString()));
    }
}
