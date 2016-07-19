/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.requester;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.module.http.api.HttpHeaders;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class HttpRequestFormParamsTestCase extends AbstractHttpRequestTestCase
{
    private static final String URL_ENCODED_STRING = "testName1=testValue1&testName2=testValue2";

    @Override
    protected String getConfigFile()
    {
        return "http-request-form-params-config.xml";
    }

    @Test
    public void sendsMapAsUrlEncodedBody() throws Exception
    {
        Map<String, String> params = new HashMap<>();
        params.put("testName1", "testValue1");
        params.put("testName2", "testValue2");
        flowRunner("formParam").withPayload(params).run();

        assertThat(uri, equalTo("/testPath"));
        assertThat(body, equalTo("testName1=testValue1&testName2=testValue2"));
        assertThat(getFirstReceivedHeader(HttpHeaders.Names.CONTENT_TYPE), equalTo(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED.toRfcString()));
    }


    @Test
    public void convertsUrlEncodedResponseToMap() throws Exception
    {
        MuleEvent event = flowRunner("formParam").withPayload(TEST_MESSAGE).run();

        assertThat(event.getMessage().getPayload(), instanceOf(Map.class));

        Map<String, String> payload = (Map<String, String>) event.getMessage().getPayload();
        assertThat(payload.size(), is(2));
        assertThat(payload.get("testName1"), equalTo("testValue1"));
        assertThat(payload.get("testName2"), equalTo("testValue2"));
    }


    @Override
    protected void writeResponse(HttpServletResponse response) throws IOException
    {
        response.setContentType(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED.toRfcString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(URL_ENCODED_STRING);
    }
}
