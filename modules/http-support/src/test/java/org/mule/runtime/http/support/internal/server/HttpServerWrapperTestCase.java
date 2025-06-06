/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.sdk.api.http.server.RequestHandler;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpServerWrapperTestCase {

  @Mock
  private org.mule.runtime.http.api.server.HttpServer delegateServer;

  @Mock
  private org.mule.runtime.http.api.server.ServerAddress muleServerAddress;

  @Mock
  private org.mule.runtime.http.api.server.RequestHandlerManager muleEndpointHandler;

  @Mock
  private org.mule.runtime.http.api.sse.server.SseEndpointManager muleSseEndpointManager;

  @Mock
  private RequestHandler requestHandler;

  @Mock
  private TlsContextFactory tlsContextFactory;

  private HttpServerWrapper wrapper;

  @BeforeEach
  void setUp() {
    when(delegateServer.getServerAddress()).thenReturn(muleServerAddress);
    when(delegateServer.getProtocol()).thenReturn(HttpConstants.Protocol.HTTP);
    wrapper = new HttpServerWrapper(delegateServer);
  }

  @Test
  void start() throws IOException {
    wrapper.start();
    verify(delegateServer).start();
  }

  @Test
  void stop() {
    wrapper.stop();
    verify(delegateServer).stop();
  }

  @Test
  void dispose() {
    wrapper.dispose();
    verify(delegateServer).dispose();
  }

  @Test
  void getServerAddress() {
    wrapper.getServerAddress();
    verify(delegateServer).getServerAddress();
  }

  @Test
  void getProtocol() {
    wrapper.getProtocol();
    verify(delegateServer).getProtocol();
  }

  @Test
  void isStopping() {
    when(delegateServer.isStopping()).thenReturn(true);
    boolean stopping = wrapper.isStopping();
    assertThat(stopping, is(true));
    verify(delegateServer).isStopping();
  }

  @Test
  void isStopped() {
    when(delegateServer.isStopped()).thenReturn(false);
    boolean stopped = wrapper.isStopped();
    assertThat(stopped, is(false));
    verify(delegateServer).isStopped();
  }

  @Test
  void enableTls() {
    wrapper.enableTls(tlsContextFactory);
    verify(delegateServer).enableTls(tlsContextFactory);
  }

  @Test
  void disableTls() {
    wrapper.disableTls();
    verify(delegateServer).disableTls();
  }

  @Test
  void addRequestHandlerWithMethods() {
    when(delegateServer.addRequestHandler(anyCollection(), anyString(), any())).thenReturn(muleEndpointHandler);
    wrapper.addRequestHandler(Arrays.asList("GET", "POST"), "/test", requestHandler);
    verify(delegateServer).addRequestHandler(anyCollection(), anyString(), any());
  }

  @Test
  void addRequestHandlerWithoutMethods() {
    when(delegateServer.addRequestHandler(anyString(), any())).thenReturn(muleEndpointHandler);
    wrapper.addRequestHandler("/test", requestHandler);
    verify(delegateServer).addRequestHandler(anyString(), any());
  }

  @Test
  void testSse() {
    when(delegateServer.sse(anyString(), any(), any())).thenReturn(muleSseEndpointManager);
    wrapper.sse("/sse", context -> {
    }, client -> {
    });
    verify(delegateServer).sse(anyString(), any(), any());
  }
}
