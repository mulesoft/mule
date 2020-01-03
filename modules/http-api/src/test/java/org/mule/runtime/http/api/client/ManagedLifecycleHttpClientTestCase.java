/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ManagedLifecycleHttpClientTestCase {

  @Mock
  private HttpRequest request;

  @Mock
  private HttpRequestOptions options;

  @Mock
  private HttpResponse httpResponse;

  @Mock
  private HttpClient httpClient;

  private ManagedLifecycleHttpClient decorator;

  @Before
  public void before() throws Exception {
    when(httpClient.sendAsync(request, options)).thenReturn(completedFuture(httpResponse));
    when(httpClient.send(request, options)).thenReturn(httpResponse);
    decorator = new ManagedLifecycleHttpClient(httpClient);
  }

  @After
  public void after() {
    verify(httpClient, never()).start();
    verify(httpClient, never()).stop();
  }

  @Test
  public void start() {
    decorator.start();
  }

  @Test
  public void stop() {
    decorator.stop();
  }

  @Test
  public void sendAsync() throws Exception {
    CompletableFuture<HttpResponse> f = decorator.sendAsync(request, options);
    verify(httpClient).sendAsync(request, options);
    assertThat(f.get(), is(sameInstance(httpResponse)));
  }

  @Test
  public void send() throws Exception {
    HttpResponse response = decorator.send(request, options);
    verify(httpClient).send(request, options);
    assertThat(response, is(sameInstance(httpResponse)));
  }
}
