/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerExpressionFilterTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-expression-filter-config.xml";
    }

    @Test
    public void returnsEmptyResponseWhenFilteringMessage() throws Exception
    {
        sendRequestAndAssertResponse(false, "");
    }

    @Test
    public void returnsExpectedResponseWhenMessageIsNotFiltered() throws Exception
    {
        sendRequestAndAssertResponse(true, TEST_MESSAGE);
    }

    private void sendRequestAndAssertResponse(boolean filterExpression, String expectedBody) throws IOException
    {
        Request request = Request.Post(String.format("http://localhost:%s", listenPort.getValue()))
                .body(new StringEntity(TEST_MESSAGE))
                .addHeader("filterExpression", Boolean.toString(filterExpression));

        HttpResponse response = request.execute().returnResponse();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        assertThat(IOUtils.toString(response.getEntity().getContent()), equalTo(expectedBody));
    }
}