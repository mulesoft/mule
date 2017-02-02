/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.services.http;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.client.HttpRequestAuthentication;
import org.mule.service.http.api.client.async.ResponseHandler;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.impl.service.HttpServiceImplementation;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.rules.ExternalResource;

/**
 * Defines a {@link org.mule.service.http.api.client.HttpClient} using a default implementation of {@link HttpService}
 *
 * <p/>
 * This rule is intended to simplify the usage of the {@link org.mule.service.http.api.client.HttpClient} as it
 * will be started/stopped as part of the test lifecycle.
 */
public class TestHttpClient extends ExternalResource implements org.mule.service.http.api.client.HttpClient {

  private final HttpService httpService;
  private TlsContextFactory tlsContextFactory;
  private org.mule.service.http.api.client.HttpClient httpClient;

  private TestHttpClient() {
    this(new HttpServiceImplementation());
  }

  private TestHttpClient(HttpService httpService) {
    checkArgument(httpService != null, "httpService cannot be null");
    this.httpService = httpService;
  }

  @Override
  protected void before() throws Throwable {
    HttpClientConfiguration.Builder builder = new HttpClientConfiguration.Builder();
    if (tlsContextFactory != null) {
      builder.setTlsContextFactory(tlsContextFactory);
    }
    HttpClientConfiguration configuration = builder.build();
    httpClient = httpService.getClientFactory().create(configuration);
    httpClient.start();
  }

  @Override
  protected void after() {
    if (httpClient != null) {
      httpClient.stop();
    }
  }

  @Override
  public void start() {
    httpClient.start();
  }

  @Override
  public void stop() {
    httpClient.stop();
  }

  @Override
  public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects,
                           HttpRequestAuthentication authentication)
      throws IOException, TimeoutException {
    return httpClient.send(request, responseTimeout, followRedirects, authentication);
  }

  @Override
  public void send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication,
                   ResponseHandler handler) {
    httpClient.send(request, responseTimeout, followRedirects, authentication, handler);
  }

  public static class Builder {

    private final HttpService service;
    private TlsContextFactory tlsContextFactory;

    /**
     * Creates a builder using a default {@link HttpService}
     */
    public Builder() {
      this.service = null;
    }

    /**
     * Creates a builder using a custom {@link HttpService}
     *
     * @param httpService httpService instance that will be used on the client. Non null
     */
    public Builder(HttpService httpService) {
      this.service = httpService;
    }

    /**
     * @param tlsContextFactory the tls context factory for creating the context to secure the connection
     * @return same builder instance
     */
    public Builder tlsContextFactory(TlsContextFactory tlsContextFactory) {
      this.tlsContextFactory = tlsContextFactory;

      return this;
    }

    /**
     * Builds the client
     *
     * @return a non null {@link TestHttpClient} with the provided configuration
     */
    public TestHttpClient build() {
      TestHttpClient httpClient;
      if (service == null) {
        httpClient = new TestHttpClient();
      } else {
        httpClient = new TestHttpClient(service);
      }

      httpClient.tlsContextFactory = tlsContextFactory;

      return httpClient;
    }
  }
}
