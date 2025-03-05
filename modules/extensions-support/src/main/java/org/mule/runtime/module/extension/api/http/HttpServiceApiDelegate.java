/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.client.auth.HttpAuthentication;
import org.mule.runtime.http.api.client.proxy.ProxyConfig;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.server.HttpServerFactory;
import org.mule.sdk.api.http.HttpClientFactory;
import org.mule.sdk.api.http.HttpRequestOptionsBuilder;
import org.mule.sdk.api.http.HttpServiceApi;

import javax.inject.Inject;

/**
 * Definition of {@link HttpServiceApi} that just delegates all to the {@link HttpService}.
 */
public class HttpServiceApiDelegate implements
    HttpServiceApi<HttpClientFactory<HttpClientConfiguration, HttpRequest, HttpRequestOptions, HttpResponse>, HttpServerFactory, HttpAuthentication, ProxyConfig> {

  @Inject
  private HttpService delegate;

  @Override
  public HttpClientFactory<HttpClientConfiguration, HttpRequest, HttpRequestOptions, HttpResponse> getClientFactory() {
    return new HttpClientFactoryWrapper(delegate.getClientFactory());
  }

  @Override
  public HttpServerFactory getServerFactory() {
    return delegate.getServerFactory();
  }

  @Override
  public HttpRequestOptionsBuilder<HttpAuthentication, ProxyConfig> requestOptionsBuilder() {
    return HttpRequestOptions.builder();
  }
}
