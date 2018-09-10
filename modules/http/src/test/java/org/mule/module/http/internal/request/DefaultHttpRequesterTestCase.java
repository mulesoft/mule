/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.ValueHolder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DefaultHttpRequesterTestCase extends AbstractMuleContextTestCase
{
    private static final String TEST_HOST = "TEST_HOST";
    private static final String TEST_PORT = "TEST_PORT";

    private static final String INVALID_PORT = "-703";
    private static final String INVALID_PORT_EXPRESSION = "#['"+ INVALID_PORT + "']";
    private static final String NULL_PORT_EXPRESSION = "#[flowVars.nullVariable]";

    private static final String HTTP_DEFAULT_URI = "http://" + TEST_HOST + ":" + HTTP.getDefaultPort() + "/";
    private static final String HTTPS_DEFAULT_URI = "http://" + TEST_HOST + ":" + HTTPS.getDefaultPort() + "/";

    private final ValueHolder<String> uriValueHolder = new ValueHolder<>();

    private DefaultHttpRequester requester = new DefaultHttpRequester();

    private DefaultHttpRequesterConfig config = spy(new DefaultHttpRequesterConfig());

    @Before
    public void setup() throws Exception
    {
        requester.setMuleContext(muleContext);
        config.setMuleContext(muleContext);
        requester.setConfig(config);
        requester.setPath("/");

        HttpClient mockedHttpClient = mock(HttpClient.class);
        final HttpResponse mockedHttpResponse = mock(HttpResponse.class);
        InputStreamHttpEntity mockedHttpEntity = mock(InputStreamHttpEntity.class);
        when(mockedHttpEntity.getInputStream()).thenReturn(null);
        when(mockedHttpResponse.getEntity()).thenReturn(mockedHttpEntity);
        when(mockedHttpClient.send((HttpRequest) anyObject(), anyInt(), anyBoolean(), (HttpRequestAuthentication) anyObject())).thenAnswer(new Answer<HttpResponse>() {
            @Override
            public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                uriValueHolder.set(((HttpRequest)invocation.getArguments()[0]).getUri());
                return mockedHttpResponse;
            }
        });
        when(config.getHttpClient()).thenReturn(mockedHttpClient);
    }

    @Test
    public void initializesWithHostAndPortInRequesterConfig() throws InitialisationException
    {
        config.setHost(TEST_HOST);
        config.setPort(TEST_PORT);
        requester.initialise();
        assertThat(requester.getHost(), equalTo(TEST_HOST));
        assertThat(requester.getPort(), equalTo(TEST_PORT));
    }

    @Test
    public void initializesWithHostAndPortInRequester() throws InitialisationException
    {
        requester.setHost(TEST_HOST);
        requester.setPort(TEST_PORT);
        requester.initialise();
        assertThat(requester.getHost(), equalTo(TEST_HOST));
        assertThat(requester.getPort(), equalTo(TEST_PORT));
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitialiseWithoutHost() throws InitialisationException
    {
        config.setHost(null);
        config.setPort(TEST_PORT);
        requester.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithoutPort() throws InitialisationException
    {
        config.setHost(TEST_HOST);
        config.setPort(null);
        requester.initialise();
    }

    private void executeRequest(String configuredPort) throws Exception{
        config.setHost(TEST_HOST);
        config.setPort(configuredPort);
        requester.initialise();
        requester.process(getTestEvent(null));
    }

    private void assertPortResolvedHTTP(String configuredPort) throws Exception {
        config.setProtocol(HTTP);
        executeRequest(configuredPort);
        assertThat(uriValueHolder.get(), is(equalTo(HTTP_DEFAULT_URI)));
    }

    private void assertPortResolvedHTTPS(String configuredPort) throws Exception {
        config.setProtocol(HTTPS);
        executeRequest(configuredPort);
        assertThat(uriValueHolder.get(), is(equalTo(HTTPS_DEFAULT_URI)));
    }

    @Test
    public void validPortWithHttp() throws Exception
    {
        assertPortResolvedHTTP("80");
    }

    @Test
    public void validPortWithHttps() throws Exception
    {
        assertPortResolvedHTTPS("443");
    }

    @Test
    public void invalidPortWithHttp() throws Exception
    {
        assertPortResolvedHTTP(INVALID_PORT);
    }

    @Test
    public void invalidPortWithHttps() throws Exception
    {
        assertPortResolvedHTTPS(INVALID_PORT);
    }

    @Test
    public void invalidPortExpressionWithHttp() throws Exception
    {
        assertPortResolvedHTTP(INVALID_PORT_EXPRESSION);
    }

    @Test
    public void invalidPortExpressionWithHttps() throws Exception
    {
        assertPortResolvedHTTPS(INVALID_PORT_EXPRESSION);
    }

    @Test
    public void nullPortExpressionWithHttp() throws Exception
    {
        assertPortResolvedHTTP(NULL_PORT_EXPRESSION);
    }

    @Test
    public void nullPortExpressionWithHttps() throws Exception
    {
        assertPortResolvedHTTPS(NULL_PORT_EXPRESSION);
    }

}
