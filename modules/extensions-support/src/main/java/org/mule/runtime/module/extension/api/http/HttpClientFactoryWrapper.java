/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.HttpClient;
import org.mule.sdk.api.http.HttpClientFactory;

public class HttpClientFactoryWrapper
    implements HttpClientFactory<HttpClientConfiguration, HttpRequest, HttpRequestOptions, HttpResponse> {

  private final org.mule.runtime.http.api.client.HttpClientFactory delegate;

  public HttpClientFactoryWrapper(org.mule.runtime.http.api.client.HttpClientFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public HttpClient<HttpRequest, HttpRequestOptions, HttpResponse> create(HttpClientConfiguration configuration) {
    return new HttpClientWrapper(delegate.create(configuration));
  }
}
