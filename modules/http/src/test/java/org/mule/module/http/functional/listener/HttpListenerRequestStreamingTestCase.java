/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleEventContext;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerRequestStreamingTestCase extends FunctionalTestCase
{

    private static final String LARGE_MESSAGE = RandomStringUtils.randomAlphanumeric(100 * 1024);

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    private String flowReceivedMessage;

    @Override
    protected String getConfigFile()
    {
        return "http-listener-request-streaming-config.xml";
    }

    @Test
    public void listenerReceivedChunkedRequest() throws Exception
    {
        String url = format("http://localhost:%s/", listenPort.getNumber());
        getFunctionalTestComponent("defaultFlow").setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                flowReceivedMessage = context.getMessageAsString();
            }
        });
        testChunkedRequestContentAndResponse(url);
        //We check twice to verify that the chunked request is consumed completely. Otherwise second request would fail
        testChunkedRequestContentAndResponse(url);
    }

    private void testChunkedRequestContentAndResponse(String url) throws Exception
    {
        final HttpRequestOptions requestOptions = newOptions().method(POST.name()).build();
        muleContext.getClient().send(url, getTestMuleMessage(new ByteArrayInputStream(LARGE_MESSAGE.getBytes())), requestOptions);
        assertThat(flowReceivedMessage, is(LARGE_MESSAGE));
    }

}
