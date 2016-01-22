/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_LISTENER_PATH;
import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_SCHEME;
import static org.mule.module.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.module.http.api.HttpHeaders.Names.HOST;
import static org.mule.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.module.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.module.http.api.HttpHeaders.Values.CLOSE;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestHeadersTestCase extends AbstractHttpRequestTestCase
{

    @Rule
    public SystemProperty host = new SystemProperty("host", "localhost");
    @Rule
    public SystemProperty encoding = new SystemProperty("encoding" , CHUNKED);

    @Override
    protected String getConfigFile()
    {
        return "http-request-headers-config.xml";
    }

    @Test
    public void sendsHeadersFromList() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("headerList");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event.getMessage().setInvocationProperty("headerName", "testName2");
        event.getMessage().setInvocationProperty("headerValue", "testValue2");

        flow.process(event);

        assertThat(getFirstReceivedHeader("testName1"), equalTo("testValue1"));
        assertThat(getFirstReceivedHeader("testName2"), equalTo("testValue2"));
    }

    @Test
    public void sendsHeadersFromMap() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("headerMap");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();

        params.put("testName1", "testValue1");
        params.put("testName2", "testValue2");

        event.getMessage().setInvocationProperty("headers", params);

        flow.process(event);

        assertThat(getFirstReceivedHeader("testName1"), equalTo("testValue1"));
        assertThat(getFirstReceivedHeader("testName2"), equalTo("testValue2"));
    }

    @Test
    public void overridesHeaders() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("headerOverride");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();

        params.put("testName1", "testValueNew");
        params.put("testName2", "testValue2");

        event.getMessage().setInvocationProperty("headers", params);

        flow.process(event);

        final Collection<String> values = headers.get("testName1");
        assertThat(values, Matchers.containsInAnyOrder(Arrays.asList("testValue1", "testValueNew").toArray(new String[2])));
        assertThat(getFirstReceivedHeader("testName2"), equalTo("testValue2"));
    }

    @Test
    public void sendsOutboundPropertiesAsHeaders() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("headerMap");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();
        params.put("testName1", "testValue1");

        event.getMessage().setInvocationProperty("headers", params);
        event.getMessage().setOutboundProperty("testName2", "testValue2");

        flow.process(event);

        assertThat(getFirstReceivedHeader("testName1"), equalTo("testValue1"));
        assertThat(getFirstReceivedHeader("testName2"), equalTo("testValue2"));
    }

    @Test
    public void allowsUserAgentOverride() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("headerMap");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();
        params.put("User-Agent", "TEST");
        event.getMessage().setInvocationProperty("headers", params);

        flow.process(event);

        assertThat(getFirstReceivedHeader("User-Agent"), equalTo("TEST"));
    }

    @Test
    public void ignoresHttpOutboundPropertiesButAcceptsHeaders() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(HTTP_LISTENER_PATH, "listenerPath");
        processEventInFlow(event, "httpHeaders");

        assertThat(getFirstReceivedHeader(HTTP_SCHEME), is("testValue1"));
        assertThat(headers.asMap(), not(hasKey(HTTP_LISTENER_PATH)));
    }

    @Test
    public void acceptsConnectionHeader() throws Exception
    {
        processEventInFlow(getTestEvent(TEST_MESSAGE), "connectionHeader");

        assertThat(getFirstReceivedHeader(CONNECTION), is(CLOSE));
    }

    @Test
    public void ignoresConnectionOutboundProperty() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(CONNECTION, CLOSE);
        processEventInFlow(event, "outboundProperties");

        assertThat(getFirstReceivedHeader(CONNECTION), is(not(CLOSE)));
    }

    @Test
    public void acceptsHostHeader() throws Exception
    {
        processEventInFlow(getTestEvent(TEST_MESSAGE), "hostHeader");

        assertThat(getFirstReceivedHeader(HOST), is(host.getValue()));
    }

    @Test
    public void ignoresHostOutboundProperty() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(HOST, host.getValue());
        processEventInFlow(event, "outboundProperties");

        assertThat(getFirstReceivedHeader(HOST), is(not(host.getValue())));
    }

    @Test
    public void acceptsTransferEncodingHeader() throws Exception
    {
        processEventInFlow(getTestEvent(TEST_MESSAGE), "transferEncodingHeader");

        assertThat(getFirstReceivedHeader(TRANSFER_ENCODING), is(encoding.getValue()));
    }

    @Test
    public void ignoresTransferEncodingOutboundProperty() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(TRANSFER_ENCODING, encoding.getValue());
        processEventInFlow(event, "outboundProperties");

        assertThat(headers.asMap(), not(hasKey(TRANSFER_ENCODING)));
    }

    private void processEventInFlow(MuleEvent event, String flowName) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        flow.process(event);
    }


}


