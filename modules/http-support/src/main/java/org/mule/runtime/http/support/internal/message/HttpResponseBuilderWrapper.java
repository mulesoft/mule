/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.support.internal.message.muletosdk.HttpResponseWrapper;
import org.mule.runtime.http.support.internal.message.sdktomule.HttpEntityWrapper;
import org.mule.sdk.api.http.domain.entity.HttpEntity;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.domain.message.response.HttpResponseBuilder;

import java.util.Collection;
import java.util.Optional;

public record HttpResponseBuilderWrapper(org.mule.runtime.http.api.domain.message.response.HttpResponseBuilder builder)
    implements HttpResponseBuilder {

  @Override
  public HttpResponseBuilder statusCode(Integer statusCode) {
    builder.statusCode(statusCode);
    return this;
  }

  @Override
  public HttpResponseBuilder reasonPhrase(String reasonPhrase) {
    builder.reasonPhrase(reasonPhrase);
    return this;
  }

  @Override
  public int getStatusCode() {
    return builder.getStatusCode();
  }

  @Override
  public String getReasonPhrase() {
    return builder.getReasonPhrase();
  }

  @Override
  public HttpResponseBuilder entity(HttpEntity entity) {
    builder.entity(new HttpEntityWrapper(entity));
    return this;
  }

  @Override
  public HttpResponseBuilder headers(MultiMap<String, String> headersMap) {
    builder.headers(headersMap);
    return this;
  }

  @Override
  public HttpResponseBuilder addHeader(String name, String value) {
    builder.addHeader(name, value);
    return this;
  }

  @Override
  public HttpResponseBuilder addHeaders(String name, Collection<String> values) {
    builder.addHeaders(name, values);
    return this;
  }

  @Override
  public HttpResponseBuilder removeHeader(String name) {
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
  public HttpResponse build() {
    return new HttpResponseWrapper(builder.build());
  }
}
