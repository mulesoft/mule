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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 *
 */
public class HttpRequestUriParamsTestCase extends AbstractHttpRequestTestCase
{

    public HttpRequestUriParamsTestCase(boolean nonBlocking)
    {
        super(nonBlocking);
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-uri-params-config.xml";
    }

    @Test
    public void sendsUriParamsFromList() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("uriParamList");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event.getMessage().setInvocationProperty("paramName", "testParam2");
        event.getMessage().setInvocationProperty("paramValue", "testValue2");

        flow.process(event);

        assertThat(uri, equalTo("/testPath/testValue1/testValue2"));
    }

    @Test
    public void sendsUriParamsFromMap() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("uriParamMap");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();

        params.put("testParam1", "testValue1");
        params.put("testParam2", "testValue2");

        event.getMessage().setInvocationProperty("params", params);

        flow.process(event);

        assertThat(uri, equalTo("/testPath/testValue1/testValue2"));
    }

    @Test
    public void overridesUriParams() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("uriParamOverride");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();

        params.put("testParam1", "testValueNew");
        params.put("testParam2", "testValue2");

        event.getMessage().setInvocationProperty("params", params);

        flow.process(event);

        assertThat(uri, equalTo("/testPath/testValueNew/testValue2"));
    }

}
