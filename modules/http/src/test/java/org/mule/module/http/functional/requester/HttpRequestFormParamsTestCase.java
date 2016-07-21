/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class HttpRequestFormParamsTestCase extends AbstractHttpRequestTestCase
{

    private static final String URL_ENCODED_STRING = "testName1=testValue1&testName2=testValue2";
    private static final String APPLICATION_JAVA = "application/java";

    @Override
    protected String getConfigFile()
    {
        return "http-request-form-params-config.xml";
    }

    @Test
    public void sendsMapAsUrlEncodedBody() throws Exception
    {
        MuleEvent event = getTestEvent(getTestMap());

        runAndAssert(event);
    }

    @Test
    public void sendsJavaMapAsUrlEncodedBody() throws Exception
    {
        MuleEvent event = getTestEvent(getTestMap());
        event.getMessage().getDataType().setMimeType(APPLICATION_JAVA);

        runAndAssert(event);
    }

    @Test
    public void sendsMultipartMapAsUrlEncodedBody() throws Exception
    {
        MuleEvent event = getTestEvent(getTestMap());
        event.getMessage().getDataType().setMimeType(APPLICATION_X_WWW_FORM_URLENCODED);

        runAndAssert(event);
    }

    private Map<String, String> getTestMap()
    {
        Map<String, String> params = new HashMap<>();

        params.put("testName1", "testValue1");
        params.put("testName2", "testValue2");
        return params;
    }

    private void runAndAssert(MuleEvent event) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("formParam");
        flow.process(event);

        assertThat(uri, equalTo("/testPath"));
        assertThat(body, equalTo("testName1=testValue1&testName2=testValue2"));
        assertThat(getFirstReceivedHeader("Content-Type"), startsWith(APPLICATION_X_WWW_FORM_URLENCODED));
    }

    @Test
    public void convertsUrlEncodedResponseToMap() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("formParam");
        MuleEvent event = flow.process(getTestEvent(TEST_MESSAGE));

        assertThat(event.getMessage().getPayload(), instanceOf(Map.class));

        Map<String, String> payload = (Map<String, String>) event.getMessage().getPayload();
        assertThat(payload.size(), is(2));
        assertThat(payload.get("testName1"), equalTo("testValue1"));
        assertThat(payload.get("testName2"), equalTo("testValue2"));
    }

    @Override
    protected void writeResponse(HttpServletResponse response) throws IOException
    {
        response.setContentType(APPLICATION_X_WWW_FORM_URLENCODED);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(URL_ENCODED_STRING);
    }
}
