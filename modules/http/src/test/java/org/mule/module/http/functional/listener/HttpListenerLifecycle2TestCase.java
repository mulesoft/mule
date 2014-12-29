/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.construct.Flow;
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.internal.listener.HttpListenerConnectionManager;
import org.mule.module.http.internal.listener.NoListenerRequestHandler;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.listener.FlowExecutionListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerLifecycle2TestCase extends FunctionalTestCase
{

    private static final String LARGE_MESSAGE = RandomStringUtils.randomAlphanumeric(100 * 1024);

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-lifecycle-2-config.xml";
    }

    private ByteArrayOutputStream response = new ByteArrayOutputStream();
    private int responseStatus;

    private FlowExecutionListener flowExecutionListener;


    @Before
    public void before()
    {
        flowExecutionListener = new FlowExecutionListener("defaultFlow", muleContext).setTimeoutInMillis(2000)
                .setNumberOfExecutionsRequired(1);
    }

    @Test
    public void listenerStoppedBefore() throws Exception
    {
        ((HttpListener) ((Flow) getFlowConstruct("defaultFlow")).getMessageSource()).stop();
        assertThat(makeHttpRequest(), is(equalTo(503)));
        assertThat(response.toByteArray(), is(equalTo("".getBytes())));
    }

    @Test
    public void listenerStoppedDuring() throws Exception
    {
        makeAsyncHttpRequest();
        ((HttpListener) ((Flow) getFlowConstruct("defaultFlow")).getMessageSource()).stop();
        flowExecutionListener.waitUntilFlowIsComplete();
        assertThat(responseStatus, is(equalTo(503)));
        assertThat(response.toByteArray(), is(equalTo(LARGE_MESSAGE.getBytes())));
    }

    @Test
    public void listenerDisposedBefore() throws Exception
    {
        ((HttpListener) ((Flow) getFlowConstruct("defaultFlow")).getMessageSource()).dispose();
        assertThat(makeHttpRequest(), is(equalTo(404)));
        assertThat(response.toByteArray(), is(equalTo(NoListenerRequestHandler.RESOURCE_NOT_FOUND.getBytes())));
    }

    @Test
    public void listenerDisposedDuring() throws Exception
    {
        makeAsyncHttpRequest();
        ((HttpListener) ((Flow) getFlowConstruct("defaultFlow")).getMessageSource()).dispose();
        flowExecutionListener.waitUntilFlowIsComplete();
        assertThat(response.toByteArray(), is(equalTo(LARGE_MESSAGE.getBytes())));
    }

    @Test
    public void flowStoppedBefore() throws Exception
    {
        ((Flow) getFlowConstruct("defaultFlow")).stop();
        assertThat(makeHttpRequest(), is(equalTo(503)));
        assertThat(response.toByteArray(), is(equalTo("".getBytes())));
    }

    @Test
    public void flowStoppedDuring() throws Exception
    {
        makeAsyncHttpRequest();
        ((Flow) getFlowConstruct("defaultFlow")).stop();
        flowExecutionListener.waitUntilFlowIsComplete();
        assertThat(responseStatus, is(equalTo(503)));
        assertThat(response.toByteArray(), is(equalTo(LARGE_MESSAGE.getBytes())));
    }

    @Test
    public void flowDisposedBefore() throws Exception
    {
        ((Flow) getFlowConstruct("defaultFlow")).dispose();
        assertThat(makeHttpRequest(), is(equalTo(404)));
        assertThat(response.toByteArray(), is(equalTo(NoListenerRequestHandler.RESOURCE_NOT_FOUND.getBytes())));
    }

    @Test
    public void flowDisposedDuring() throws Exception
    {
        makeAsyncHttpRequest();
        ((Flow) getFlowConstruct("defaultFlow")).dispose();
        flowExecutionListener.waitUntilFlowIsComplete();
        assertThat(responseStatus, is(equalTo(503)));
        assertThat(response.toByteArray(), is(equalTo(LARGE_MESSAGE.getBytes())));
    }

    @Test(expected = HttpHostConnectException.class)
    public void listenerConfigStoppedBefore() throws Exception
    {
        ((Stoppable) muleContext.getRegistry().lookupObject("listenerConfig")).stop();
        makeHttpRequest();
    }

    @Test
    public void listenerConfigStoppedDuring() throws Exception
    {
        makeAsyncHttpRequest();
        ((Stoppable) muleContext.getRegistry().lookupObject("listenerConfig")).stop();
        flowExecutionListener.waitUntilFlowIsComplete();
        assertThat(responseStatus, is(equalTo(503)));
        assertThat(response.toByteArray(), is(equalTo(LARGE_MESSAGE.getBytes())));
    }

    @Test(expected = HttpHostConnectException.class)
    public void grizzlyDisposedBefore() throws Exception
    {
        ((Disposable) muleContext.getRegistry().lookupObject(HttpListenerConnectionManager
                                                                     .HTTP_LISTENER_CONNECTION_MANAGER)).dispose();
        makeHttpRequest();
    }

    @Test
    public void grizzlyDisposedDuring() throws Exception
    {
        makeAsyncHttpRequest();
        ((Disposable) muleContext.getRegistry().lookupObject(HttpListenerConnectionManager
                                                                     .HTTP_LISTENER_CONNECTION_MANAGER)).dispose();
        flowExecutionListener.waitUntilFlowIsComplete();
        assertThat(responseStatus, is(equalTo(503)));
        assertThat(response.toByteArray(), is(equalTo(LARGE_MESSAGE.getBytes())));
    }

    @Test(expected = HttpHostConnectException.class)
    public void muleContexStoppedBefore() throws Exception
    {
        muleContext.stop();
        makeHttpRequest();
    }

    @Test
    public void muleContextStoppedDuring() throws Exception
    {
        makeAsyncHttpRequest();
        muleContext.stop();
        flowExecutionListener.waitUntilFlowIsComplete();
        assertThat(responseStatus, is(equalTo(503)));
        assertThat(response.toByteArray(), is(equalTo(LARGE_MESSAGE.getBytes())));
    }


    @Test(expected = HttpHostConnectException.class)
    public void muleContextDisposedBefore() throws Exception
    {
        muleContext.dispose();
        makeHttpRequest();
    }

    @Test
    public void muleContextDisposedDuring() throws Exception
    {
        makeAsyncHttpRequest();
        muleContext.dispose();
        flowExecutionListener.waitUntilFlowIsComplete();
        assertThat(responseStatus, is(equalTo(503)));
        assertThat(response.toByteArray(), is(equalTo(LARGE_MESSAGE.getBytes())));
    }

    private void makeAsyncHttpRequest() throws InterruptedException
    {
        Callable callable = new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                makeHttpRequest();
                return null;
            }
        };

        Executors.newSingleThreadExecutor().submit(callable);
        // Ensure http request gets send before continuing with test
        Thread.sleep(100);
    }

    private int makeHttpRequest() throws IOException
    {
        HttpResponse httpResponse = Request.Post(format("http://localhost:%s/",
                                                        listenPort.getNumber())).bodyByteArray(LARGE_MESSAGE.getBytes
                ()).execute().returnResponse();
        httpResponse.getEntity().writeTo(this.response);
        responseStatus = httpResponse.getStatusLine().getStatusCode();
        return responseStatus;
    }

}
