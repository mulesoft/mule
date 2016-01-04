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

public class HttpRequestQueryParamsTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-query-params-config.xml";
    }

    @Test
    public void sendsQueryParamsFromList() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("queryParamList");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event.getMessage().setInvocationProperty("paramName", "testName2");
        event.getMessage().setInvocationProperty("paramValue", "testValue2");

        flow.process(event);

        assertThat(uri, equalTo("/testPath?testName1=testValue1&testName2=testValue2"));
    }

    @Test
    public void sendsQueryParamsFromMap() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("queryParamMap");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();

        params.put("testName1", "testValue1");
        params.put("testName2", "testValue2");

        event.getMessage().setInvocationProperty("params", params);

        flow.process(event);

        assertThat(uri, equalTo("/testPath?testName1=testValue1&testName2=testValue2"));
    }

    @Test
    public void queryParamsOverride() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("multipleQueryParam");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();

        params.put("testName1", "testValueNew");
        params.put("testName2", "testValue2");

        event.getMessage().setInvocationProperty("params", params);

        flow.process(event);

        assertThat(uri, equalTo("/testPath?testName1=testValue1&testName1=testValueNew&testName2=testValue2"));
    }

    @Test
    public void sendsQueryParamsNulls() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("queryParamNulls");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        flow.process(event);

        assertThat(uri, equalTo("/testPath?testName1&testName2"));
    }

}
