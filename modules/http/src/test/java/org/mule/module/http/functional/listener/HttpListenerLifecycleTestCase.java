/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.http.api.HttpConstants.HttpStatus.SERVICE_UNAVAILABLE;

import org.mule.construct.Flow;
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.listener.HttpListenerRegistry;
import org.mule.module.http.internal.listener.HttpListenerRegistryTestCase;
import org.mule.module.http.internal.listener.RequestHandlerManager;
import org.mule.module.http.internal.listener.ServiceTemporarilyUnavailableListenerRequestHandler;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.AcceptsAllMethodsRequestMatcher;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpListenerLifecycleTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    @Rule
    public DynamicPort port2 = new DynamicPort("port2");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "http-listener-lifecycle-config.xml";
    }

    @Test
    public void stoppedListenerReturns503() throws Exception
    {
        HttpListener httpListener = (HttpListener) ((Flow) getFlowConstruct("testPathFlow")).getMessageSource();
        httpListener.stop();
        final Response response = Request.Get(getLifecycleConfigUrl("/path/subpath")).execute();
        final HttpResponse returnResponse = response.returnResponse();
        assertThat(returnResponse.getEntity(), notNullValue());
        assertThat(returnResponse.getStatusLine().getStatusCode(), is(SERVICE_UNAVAILABLE.getStatusCode()));
        assertThat(returnResponse.getStatusLine().getReasonPhrase(), is(SERVICE_UNAVAILABLE.getReasonPhrase()));
        assertThat(getHttpEntityContent(returnResponse), startsWith("Service not available for request uri: "));
    }

    private String getHttpEntityContent(HttpResponse returnResponse) throws IOException {
        return IOUtils.toString(returnResponse.getEntity().getContent());
    }

    @Test
    public void stopOneListenerDoesNotAffectAnother() throws Exception
    {
        HttpListener httpListener = (HttpListener) ((Flow) getFlowConstruct("testPathFlow")).getMessageSource();
        httpListener.stop();
        callAndAssertResponseFromUnaffectedListener();
    }

    @Test
    public void restartListener() throws Exception
    {
        HttpListener httpListener = (HttpListener) ((Flow) getFlowConstruct("testPathFlow")).getMessageSource();
        httpListener.stop();
        httpListener.start();
        final Response response = Request.Get(getLifecycleConfigUrl("/path/subpath")).execute();
        final HttpResponse httoResponse = response.returnResponse();
        assertThat(httoResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httoResponse.getEntity().getContent()), is("ok"));
    }

    @Test
    public void disposeListenerReturns404() throws Exception
    {
        HttpListener httpListener = (HttpListener) ((Flow) getFlowConstruct("catchAllWithinTestPathFlow")).getMessageSource();
        httpListener.dispose();
        final Response response = Request.Get(getLifecycleConfigUrl("/path/somepath")).execute();
        final HttpResponse httoResponse = response.returnResponse();
        assertThat(httoResponse.getStatusLine().getStatusCode(), is(404));
    }

    @Test
    public void stoppedListenerConfigDoNotListen() throws Exception
    {
        HttpListenerConfig httpListenerConfig = muleContext.getRegistry().get("testLifecycleListenerConfig");
        httpListenerConfig.stop();
        expectedException.expect(ConnectException.class);
        Request.Get(getLifecycleConfigUrl("/path/subpath")).execute();
    }

    @Test
    public void stopOneListenerConfigDoesNotAffectAnother() throws Exception
    {
        HttpListenerConfig httpListenerConfig = muleContext.getRegistry().get("testLifecycleListenerConfig");
        httpListenerConfig.stop();
        callAndAssertResponseFromUnaffectedListener();
    }

    @Test
    public void restartListenerConfig() throws Exception
    {
        HttpListenerConfig httpListenerConfig = muleContext.getRegistry().get("testLifecycleListenerConfig");
        httpListenerConfig.stop();
        httpListenerConfig.start();
        final Response response = Request.Get(getLifecycleConfigUrl("/path/anotherPath")).execute();
        final HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("catchAll"));
    }

    private void callAndAssertResponseFromUnaffectedListener() throws IOException
    {
        final Response response = Request.Get(getUnchangedConfigUrl()).execute();
        final HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("works"));
    }

    private String getLifecycleConfigUrl(String path)
    {
        return String.format("http://localhost:%s/%s", port1.getNumber(), path);
    }

    private String getUnchangedConfigUrl()
    {
        return String.format("http://localhost:%s/%s", port2.getNumber(), "/path");
    }

}
