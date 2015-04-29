/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester;

import static java.lang.String.valueOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.module.http.api.requester.HttpSendBodyMode.ALWAYS;
import static org.mule.module.http.api.requester.HttpStreamingType.AUTO;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.ssl.api.TlsContextFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

@SmallTest
public class HttpRequesterConfigBuilderTestCase extends AbstractMuleTestCase
{

    public static final String AN_EXPRESSION = "myExpression";
    public static final String HOST = "anyHost";
    public static final int PORT = 9090;

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private TlsContextFactory mockTlsContext = mock(TlsContextFactory.class, Answers.RETURNS_DEEP_STUBS.get());
    private HttpAuthentication mockAuthentication = mock(HttpAuthentication.class, Answers.RETURNS_DEEP_STUBS.get());
    private ProxyConfig mockProxyConfig = mock(ProxyConfig.class, Answers.RETURNS_DEEP_STUBS.get());
    private HttpRequesterConfigBuilder builder = new HttpRequesterConfigBuilder(mockMuleContext);

    @Before
    public void setUp()
    {
        when(mockMuleContext.getRegistry().get(Mockito.anyString())).thenReturn(null);
    }

    @Test
    public void responseTimeout() throws Exception
    {
        int responseTimeout = 100;
        assertThat(builder.setResponseTimeout(responseTimeout).build().getResponseTimeout(), is(valueOf(responseTimeout)));
    }

    @Test
    public void responseTimeoutExpression() throws Exception
    {
        assertThat(builder.setResponseTimeoutExpression(AN_EXPRESSION).build().getResponseTimeout(), is(AN_EXPRESSION));
    }

    @Test(expected = MuleException.class)
    public void tlsContextWithoutHttps() throws Exception
    {
        assertThat(builder.setTlsContext(mockTlsContext).build().getTlsContext(), is(mockTlsContext ));
    }

    @Test
    public void tlsContext() throws Exception
    {
        assertThat(builder.setProtocol(HTTPS).setTlsContext(mockTlsContext).build().getTlsContext(), is(mockTlsContext));
    }

    @Test
    public void basicAuthentication() throws Exception
    {
        assertThat(builder.setAuthentication(mockAuthentication).build().getAuthentication(), is(mockAuthentication));
    }

    @Test
    public void proxy() throws Exception
    {
        when(mockProxyConfig.getHost()).thenReturn(HOST);
        when(mockProxyConfig.getPort()).thenReturn(PORT);
        assertThat(builder.setProxyConfig(mockProxyConfig).build().getProxyConfig(), is(mockProxyConfig));
    }

    @Test
    public void sendBodyMode() throws Exception
    {
        assertThat(builder.setSendBodyMode(ALWAYS).build().getSendBodyMode(), is(ALWAYS.name()));
    }

    @Test
    public void sendBodyModeExpression() throws Exception
    {
        assertThat(builder.setSendBodyModeExpression(AN_EXPRESSION).build().getSendBodyMode(), is(AN_EXPRESSION));
    }

    @Test
    public void setRequestStreamingMode() throws Exception
    {
        assertThat(builder.setRequestStreamingMode(AUTO).build().getRequestStreamingMode(), is(AUTO.name()));
    }

    @Test
    public void setRequestStreamingModeExpression() throws Exception
    {
        assertThat(builder.setRequestStreamingModeExpression(AN_EXPRESSION).build().getRequestStreamingMode(), is(AN_EXPRESSION));
    }

    @Test
    public void parseResponse() throws Exception
    {
        assertThat(builder.setParseResponse(true).build().getParseResponse(), is("true"));
    }

    @Test
    public void parseResponseExpression() throws Exception
    {
        assertThat(builder.setParseResponseExpression(AN_EXPRESSION).build().getParseResponse(), is(AN_EXPRESSION));
    }

    @Test
    public void host() throws Exception
    {
        assertThat(builder.setHostExpression(HOST).build().getHost(), is(HOST));
    }

    @Test
    public void port() throws Exception
    {
        assertThat(builder.setPort(PORT).build().getPort(), is(valueOf(PORT)));
    }

    @Test
    public void portExpression() throws Exception
    {
        assertThat(builder.setPortExpression(AN_EXPRESSION).build().getPort(), is(valueOf(AN_EXPRESSION)));
    }

}