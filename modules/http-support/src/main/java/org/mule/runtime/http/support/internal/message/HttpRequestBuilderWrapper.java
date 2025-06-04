/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.support.internal.message.muletosdk.HttpRequestWrapper;
import org.mule.runtime.http.support.internal.message.sdktomule.HttpEntityWrapper;
import org.mule.sdk.api.http.HttpConstants;
import org.mule.sdk.api.http.domain.HttpProtocol;
import org.mule.sdk.api.http.domain.entity.HttpEntity;
import org.mule.sdk.api.http.domain.message.request.HttpRequest;
import org.mule.sdk.api.http.domain.message.request.HttpRequestBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;

public record HttpRequestBuilderWrapper(org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder builder)
    implements HttpRequestBuilder {

  @Override
  public HttpRequestBuilder uri(String uri) {
    builder.uri(uri);
    return this;
  }

  @Override
  public HttpRequestBuilder uri(URI uri) {
    builder.uri(uri);
    return this;
  }

  @Override
  public HttpRequestBuilder method(String method) {
    builder.method(method);
    return this;
  }

  @Override
  public HttpRequestBuilder method(HttpConstants.Method method) {
    builder.method(method.name());
    return this;
  }

  @Override
  public HttpRequestBuilder protocol(HttpProtocol protocol) {
    builder.protocol(org.mule.runtime.http.api.domain.HttpProtocol.valueOf(protocol.name()));
    return this;
  }

  @Override
  public HttpRequestBuilder queryParams(MultiMap<String, String> queryParams) {
    builder.queryParams(queryParams);
    return this;
  }

  @Override
  public HttpRequestBuilder addQueryParam(String name, String value) {
    builder.addQueryParam(name, value);
    return this;
  }

  @Override
  public URI getUri() {
    return builder.getUri();
  }

  @Override
  public String getMethod() {
    return builder.getMethod();
  }

  @Override
  public MultiMap<String, String> getQueryParams() {
    return builder.getQueryParams();
  }

  @Override
  public HttpRequestBuilder entity(HttpEntity entity) {
    builder.entity(new HttpEntityWrapper(entity));
    return this;
  }

  @Override
  public HttpRequestBuilder headers(MultiMap<String, String> headersMap) {
    builder.headers(headersMap);
    return this;
  }

  @Override
  public HttpRequestBuilder addHeader(String name, String value) {
    builder.addHeader(name, value);
    return this;
  }

  @Override
  public HttpRequestBuilder addHeaders(String name, Collection<String> values) {
    builder.addHeaders(name, values);
    return this;
  }

  @Override
  public HttpRequestBuilder removeHeader(String name) {
    builder.removeHeader(name);
    return this;
  }

  @Override
  public Optional<String> getHeaderValue(String name) {
    return builder.getHeaderValue(name);
  }

  @Override
  public Collection<String> getHeaderValues(String name) {
    return builder.getHeaderValues(name);
  }

  @Override
  public MultiMap<String, String> getHeaders() {
    return builder.getHeaders();
  }

  @Override
  public HttpRequest build() {
    return new HttpRequestWrapper(builder.build());
  }
}
