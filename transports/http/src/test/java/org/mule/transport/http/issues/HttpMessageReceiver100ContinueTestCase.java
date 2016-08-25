/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.issues;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.module.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;

import org.junit.Rule;
import org.junit.Test;

public class HttpMessageReceiver100ContinueTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    @Rule
    public SystemProperty path = new SystemProperty("path", "path");


    @Override
    protected String getConfigFile()
    {
        return "http-receiver-100-continue-config-flow.xml";
    }

    @Test
    public void serverHandles100ContinueProperly() throws Exception
    {
        AsyncHttpClientConfig asyncHttpClientConfig = new AsyncHttpClientConfig.Builder().build();
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(asyncHttpClientConfig), asyncHttpClientConfig);
        ListenableFuture<com.ning.http.client.Response> responseFuture = asyncHttpClient.
                preparePost(getListenerUrl()).setBody("a=1&b=2").setHeader("Expect", "100-continue").
                addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED).execute();
        com.ning.http.client.Response response = responseFuture.get();

        assertThat(response.getStatusCode(), is(200));
    }

    private String getListenerUrl()
    {
        return String.format("http://localhost:%s/%s", listenPort.getNumber(), path.getValue());
    }

}
