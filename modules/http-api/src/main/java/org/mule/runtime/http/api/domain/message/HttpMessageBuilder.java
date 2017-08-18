/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.CaseInsensitiveMultiMap;
import org.mule.runtime.http.api.domain.entity.EmptyHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;

import java.util.Collection;
import java.util.Optional;

/**
 * Base implementation of an {@link HttpMessage} builder. Implementations should extend it and indicate their type and the type of
 * the message they build.
 *
 * @param <B> the type of the builder itself.
 * @param <M> the type of {@link HttpMessage} that the builder creates.
 */
public abstract class HttpMessageBuilder<B extends HttpMessageBuilder, M extends HttpMessage> {

  protected MultiMap<String, String> headers = new CaseInsensitiveMultiMap();
  protected HttpEntity entity = new EmptyHttpEntity();

  /**
   * @param entity the {@link HttpEntity} that should be used as body for the {@link HttpMessage}. Non null.
   * @return this builder
   */
  public B entity(HttpEntity entity) {
    checkNotNull(entity, "entity cannot be null, use an EmptyHttpEntity instead");
    this.entity = entity;
    return (B) this;
  }

  /**
   * @param headersMap a {@link MultiMap} representing the HTTP headers of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public B headers(MultiMap<String, String> headersMap) {
    headersMap.keySet().forEach(
                                key -> headersMap.getAll(key).forEach(
                                                                      value -> this.headers.put(key, value)));
    return (B) this;
  }

  /**
   * Includes a new header to be sent in the desired {@link HttpMessage}. Since HTTP headers are case insensitive and can have
   * several values, multiple calls to this method using the same header name will accumulate the values and all of them will be sent.
   *
   * @param name the name of the HTTP header
   * @param value the value of the HTTP header
   * @return this builder
   */
  public B addHeader(String name, String value) {
    headers.put(name, value);
    return (B) this;
  }

  /**
   * Removes a header. Since multiple values might be present, all are removed.
   *
   * @param name the name of the HTTP header to remove
   * @return this builder
   */
  public B removeHeader(String name) {
    headers.remove(name);
    return (B) this;
  }

  /**
   * Returns the value of a given HTTP request. If there are several then the first will be return.
   * Use {@link #getHeaderValues(String)} instead if all values are required.
   *
   * @param name the name of the desired HTTP header
   * @return the value of the header or {@code null} if there isn't one
   */
  public Optional<String> getHeaderValue(String name) {
    return ofNullable(headers.get(name));
  }

  /**
   * Gives access to all current values of a given HTTP header.
   *
   * @param name the name of the desired HTTP header
   * @return an immutable {@link Collection} of {@link String} values for the header
   */
  public Collection<String> getHeaderValues(String name) {
    return headers.getAll(name);
  }

  /**
   * @return an immutable version of the current headers in the builder.
   */
  public MultiMap<String, String> getHeaders() {
    return headers.toImmutableMultiMap();
  }

  public abstract M build();

}
