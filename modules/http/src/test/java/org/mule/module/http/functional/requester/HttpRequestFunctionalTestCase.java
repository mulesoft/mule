/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.api.transport.PropertyScope.INBOUND;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestFunctionalTestCase extends AbstractHttpRequestTestCase
{

    private static final String TEST_HEADER_NAME = "TestHeaderName";
    private static final String TEST_HEADER_VALUE = "TestHeaderValue";
    private static final String DEFAULT_PORT_HTTP_REQUEST_CONFIG_NAME = "requestConfigHttp";
    private static final String DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME = "requestConfigHttps";

    @Override
    protected String getConfigFile()
    {
        return "http-request-functional-config.xml";
    }

    @Test
    public void requestConfigDefaultPortHttp()
    {
        HttpRequesterConfig httpRequesterConfig = muleContext.getRegistry().get(DEFAULT_PORT_HTTP_REQUEST_CONFIG_NAME);
        assertThat(httpRequesterConfig.getPort(), is(String.valueOf(HTTP.getDefaultPort())));
    }

    @Test
    public void requestConfigDefaultPortHttps()
    {
        HttpRequesterConfig httpRequesterConfig = muleContext.getRegistry().get(DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME);
        assertThat(httpRequesterConfig.getPort(), is(String.valueOf(HTTPS.getDefaultPort())));
    }

    @Test
    public void requestConfigDefaultTlsContextHttps()
    {
        DefaultHttpRequesterConfig httpRequesterConfig = muleContext.getRegistry().get(DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME);
        assertThat(httpRequesterConfig.getTlsContext(), notNullValue());
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

    @Rule
    public DynamicPort blockingHttpPort = new DynamicPort("blockingHttpPort");

    @Test
    public void blockingResponseBodyIsMappedToPayload() throws Exception
    {
        MuleEvent event = runFlow("blockingRequestFlow", TEST_MESSAGE);
        assertTrue(event.getMessage().getPayload() instanceof String);
        assertThat(event.getMessage().getPayloadAsString(), equalTo("value"));
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

    @Test
    public void previousInboundPropertiesAreCleared() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setProperty("TestInboundProperty", "TestValue", INBOUND);

        Flow flow = (Flow) getFlowConstruct("requestFlow");
        event = flow.process(event);

        assertThat(event.getMessage().getInboundProperty("TestInboundProperty"), nullValue());
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.addHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
        super.handleRequest(baseRequest, request, response);
    }
}
