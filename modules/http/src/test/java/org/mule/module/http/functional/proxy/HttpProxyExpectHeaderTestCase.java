/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.proxy;

import static org.apache.http.entity.ContentType.DEFAULT_TEXT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import org.mule.module.http.functional.AbstractHttpExpectHeaderServerTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;


public class HttpProxyExpectHeaderTestCase extends AbstractHttpExpectHeaderServerTestCase
{

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    @Override
    protected String getConfigFile()
    {
        return "http-proxy-template-config.xml";
    }

    @Test
    public void handlesContinueResponse() throws Exception
    {
        startExpectContinueServer();
        Response response = sendRequest();
        assertThat(response.returnContent().asString(), equalTo(TEST_MESSAGE));
        stopServer();
    }

    @Test
    public void handlesExpectationFailedResponse() throws Exception
    {
        startExpectFailedServer();
        Response response = sendRequest();
        assertThat(response.returnResponse().getStatusLine().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
        stopServer();
    }

    private Response sendRequest() throws IOException
    {
        return Request.Post(String.format("http://localhost:%s", proxyPort.getNumber()))
                .useExpectContinue()
                .bodyString(TEST_MESSAGE, DEFAULT_TEXT)
                .connectTimeout(RECEIVE_TIMEOUT).execute();
    }

}
