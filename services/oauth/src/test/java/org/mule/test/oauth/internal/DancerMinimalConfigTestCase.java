/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.el.ExpressionEvaluator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientFactory;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerFactory;
import org.mule.service.http.api.server.RequestHandlerManager;
import org.mule.services.oauth.internal.DefaultOAuthService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.input.ReaderInputStream;
import org.junit.Before;
import org.junit.Test;

public class DancerMinimalConfigTestCase extends AbstractMuleContextTestCase {

  private OAuthService service;
  private HttpClient httpClient;

  @Before
  public void before() throws ConnectionException {
    final HttpService httpService = mock(HttpService.class);
    final HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    httpClient = mock(HttpClient.class);
    when(httpClientFactory.create(any())).thenReturn(httpClient);
    when(httpService.getClientFactory()).thenReturn(httpClientFactory);

    final HttpServerFactory httpServerFactory = mock(HttpServerFactory.class);
    final HttpServer httpServer = mock(HttpServer.class);
    when(httpServer.addRequestHandler(anyString(), any())).thenReturn(mock(RequestHandlerManager.class));
    when(httpServer.addRequestHandler(any(), anyString(), any())).thenReturn(mock(RequestHandlerManager.class));
    when(httpServerFactory.create(any())).thenReturn(httpServer);
    when(httpService.getServerFactory()).thenReturn(httpServerFactory);

    service = new DefaultOAuthService(httpService, mock(SchedulerService.class));
  }

  @Test
  public void clientCredentials() throws MuleException, IOException, TimeoutException {
    final HttpResponse httpResponse = mock(HttpResponse.class);
    final InputStreamHttpEntity httpEntity = mock(InputStreamHttpEntity.class);
    when(httpEntity.getInputStream()).thenReturn(new ReaderInputStream(new StringReader("")));
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpClient.send(any(), anyInt(), anyBoolean(), any())).thenReturn(httpResponse);

    final OAuthClientCredentialsDancerBuilder builder =
        service.clientCredentialsGrantTypeDancerBuilder(muleContext.getRegistry().lookupObject(LockFactory.class),
                                                        new HashMap<>(), mock(ExpressionEvaluator.class));

    builder.clientCredentials("clientId", "clientSecret");
    builder.tokenUrl("http://host/token");

    final OAuthDancer minimalDancer = builder.build();
    initialiseIfNeeded(minimalDancer);
    startIfNeeded(minimalDancer);
  }

  @Test
  public void authorizationCode() throws MuleException, IOException, TimeoutException {
    final HttpResponse httpResponse = mock(HttpResponse.class);
    final InputStreamHttpEntity httpEntity = mock(InputStreamHttpEntity.class);
    when(httpEntity.getInputStream()).thenReturn(new ReaderInputStream(new StringReader("")));
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpClient.send(any(), anyInt(), anyBoolean(), any())).thenReturn(httpResponse);

    final OAuthAuthorizationCodeDancerBuilder builder =
        service.authorizationCodeGrantTypeDancerBuilder(muleContext.getRegistry().lookupObject(LockFactory.class),
                                                        new HashMap<>(), mock(ExpressionEvaluator.class));

    builder.clientCredentials("clientId", "clientSecret");
    builder.tokenUrl("http://host/token");
    builder.authorizationUrl("http://host/auth");
    builder.localCallback(new URL("http://localhost:8080/localCallback"));

    final OAuthDancer minimalDancer = builder.build();
    initialiseIfNeeded(minimalDancer);
    startIfNeeded(minimalDancer);
  }
}
