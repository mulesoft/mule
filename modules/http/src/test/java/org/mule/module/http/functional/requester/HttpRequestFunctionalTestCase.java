/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleEvent;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class HttpRequestFunctionalTestCase extends AbstractHttpRequestTestCase
{
    private static final String TEST_HEADER_NAME = "TestHeaderName";
    private static final String TEST_HEADER_VALUE = "TestHeaderValue";

    @Override
    protected String getConfigFile()
    {
        return "http-request-functional-config.xml";
    }


    @Test
    public void payloadIsSentAsRequestBody() throws Exception
    {
        runFlow("requestFlow", TEST_MESSAGE);
        assertThat(body, equalTo(TEST_MESSAGE));
    }

    @Test
    public void outboundPropertiesAreSentAsHeaders() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty("TestHeader", "TestValue");
        testFlow("requestFlow", event);
        assertThat(getFirstReceivedHeader("TestHeader"), equalTo("TestValue"));
    }

    @Test
    public void responseBodyIsMappedToPayload() throws Exception
    {
        MuleEvent event = runFlow("requestFlow", TEST_MESSAGE);
        assertTrue(event.getMessage().getPayload() instanceof InputStream);
        assertThat(event.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    @Test
    public void responseStatusCodeIsSetAsInboundProperty() throws Exception
    {
        MuleEvent event = runFlow("requestFlow", TEST_MESSAGE);
        assertThat((int) event.getMessage().getInboundProperty("http.status"), CoreMatchers.is(200));
    }

    @Test
    public void responseHeadersAreMappedAsInboundProperties() throws Exception
    {
        MuleEvent event = runFlow("requestFlow", TEST_MESSAGE);
        String headerValue = event.getMessage().getInboundProperty(TEST_HEADER_NAME);
        assertThat(headerValue, equalTo(TEST_HEADER_VALUE));
    }

    @Test
    public void basePathFromConfigIsUsedInRequest() throws Exception
    {
        runFlow("requestFlow", TEST_MESSAGE);
        assertThat(uri, equalTo("/basePath/requestPath"));
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.addHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
        super.handleRequest(baseRequest, request, response);
    }
}
