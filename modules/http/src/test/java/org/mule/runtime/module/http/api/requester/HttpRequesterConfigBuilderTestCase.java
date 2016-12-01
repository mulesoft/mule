/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.api.requester;

import static java.lang.String.valueOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.runtime.module.http.api.requester.HttpSendBodyMode.ALWAYS;
import static org.mule.runtime.module.http.api.requester.HttpStreamingType.AUTO;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.http.api.HttpAuthentication;
import org.mule.service.http.api.client.proxy.ProxyConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.api.tls.TlsContextFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

@SmallTest
public class HttpRequesterConfigBuilderTestCase extends AbstractMuleTestCase {

  public static final String AN_EXPRESSION = "myExpression";
  public static final String HOST = "anyHost";
  public static final int PORT = 9090;

  private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
  private TlsContextFactory mockTlsContext = mock(TlsContextFactory.class, Answers.RETURNS_DEEP_STUBS.get());
  private HttpAuthentication mockAuthentication = mock(HttpAuthentication.class, Answers.RETURNS_DEEP_STUBS.get());
  private ProxyConfig mockProxyConfig = mock(ProxyConfig.class, Answers.RETURNS_DEEP_STUBS.get());
  private HttpRequesterConfigBuilder builder = new HttpRequesterConfigBuilder(mockMuleContext);
  private HttpRequesterConfig requestConfig;

  @Before
  public void setUp() {
    when(mockMuleContext.getRegistry().get(Mockito.anyString())).thenReturn(null);
  }

  @After
  public void tearDown() throws Exception {
    if (requestConfig != null) {
      requestConfig.stop();
    }
  }

  @Test
  public void responseTimeout() throws Exception {
    int responseTimeout = 100;
    requestConfig = builder.setResponseTimeout(responseTimeout).build();
    assertThat(requestConfig.getResponseTimeout(), is(valueOf(responseTimeout)));
  }

  @Test
  public void responseTimeoutExpression() throws Exception {
    requestConfig = builder.setResponseTimeoutExpression(AN_EXPRESSION).build();
    assertThat(requestConfig.getResponseTimeout(), is(AN_EXPRESSION));
  }

  @Test(expected = MuleException.class)
  public void tlsContextWithoutHttps() throws Exception {
    requestConfig = builder.setTlsContext(mockTlsContext).build();
    assertThat(requestConfig.getTlsContext(), is(mockTlsContext));
  }

  @Test
  public void tlsContext() throws Exception {
    requestConfig = builder.setProtocol(HTTPS).setTlsContext(mockTlsContext).build();
    assertThat(requestConfig.getTlsContext(), is(mockTlsContext));
  }

  @Test
  public void basicAuthentication() throws Exception {
    requestConfig = builder.setAuthentication(mockAuthentication).build();
    assertThat(requestConfig.getAuthentication(), is(mockAuthentication));
  }

  @Test
  public void proxy() throws Exception {
    when(mockProxyConfig.getHost()).thenReturn(HOST);
    when(mockProxyConfig.getPort()).thenReturn(PORT);
    requestConfig = builder.setProxyConfig(mockProxyConfig).build();
    assertThat(requestConfig.getProxyConfig(), is(mockProxyConfig));
  }

  @Test
  public void sendBodyMode() throws Exception {
    requestConfig = builder.setSendBodyMode(ALWAYS).build();
    assertThat(requestConfig.getSendBodyMode(), is(ALWAYS.name()));
  }

  @Test
  public void sendBodyModeExpression() throws Exception {
    requestConfig = builder.setSendBodyModeExpression(AN_EXPRESSION).build();
    assertThat(requestConfig.getSendBodyMode(), is(AN_EXPRESSION));
  }

  @Test
  public void setRequestStreamingMode() throws Exception {
    requestConfig = builder.setRequestStreamingMode(AUTO).build();
    assertThat(requestConfig.getRequestStreamingMode(), is(AUTO.name()));
  }

  @Test
  public void setRequestStreamingModeExpression() throws Exception {
    requestConfig = builder.setRequestStreamingModeExpression(AN_EXPRESSION).build();
    assertThat(requestConfig.getRequestStreamingMode(), is(AN_EXPRESSION));
  }

  @Test
  public void parseResponse() throws Exception {
    requestConfig = builder.setParseResponse(true).build();
    assertThat(requestConfig.getParseResponse(), is("true"));
  }

  @Test
  public void parseResponseExpression() throws Exception {
    requestConfig = builder.setParseResponseExpression(AN_EXPRESSION).build();
    assertThat(requestConfig.getParseResponse(), is(AN_EXPRESSION));
  }

  @Test
  public void host() throws Exception {
    requestConfig = builder.setHostExpression(HOST).build();
    assertThat(requestConfig.getHost(), is(HOST));
  }

  @Test
  public void port() throws Exception {
    requestConfig = builder.setPort(PORT).build();
    assertThat(requestConfig.getPort(), is(valueOf(PORT)));
  }

  @Test
  public void portExpression() throws Exception {
    requestConfig = builder.setPortExpression(AN_EXPRESSION).build();
    assertThat(requestConfig.getPort(), is(valueOf(AN_EXPRESSION)));
  }

}
