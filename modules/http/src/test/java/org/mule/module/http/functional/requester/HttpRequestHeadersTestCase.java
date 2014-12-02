/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

public class HttpRequestHeadersTestCase extends AbstractHttpRequestTestCase
{

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

}


