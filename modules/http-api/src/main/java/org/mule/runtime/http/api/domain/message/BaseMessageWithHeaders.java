/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.CaseInsensitiveMultiMap;

import java.util.Collection;

/**
 * Base class for implementations of {@link MessageWithHeaders}
 *
 * @since 4.2.0
 */
public abstract class BaseMessageWithHeaders implements MessageWithHeaders {

  protected CaseInsensitiveMultiMap headers;

  @Deprecated
  public BaseMessageWithHeaders(MultiMap<String, String> headers) {
    this.headers = new CaseInsensitiveMultiMap(headers);
  }

  public BaseMessageWithHeaders(CaseInsensitiveMultiMap headers) {
    this.headers = headers;
  }

  @Override
  public Collection<String> getHeaderNames() {
    return headers.keySet();
  }

  @Override
  public boolean containsHeader(String headerName) {
    return headers.containsKey(headerName);
  }

  @Override
  public String getHeaderValue(String headerName) {
    return headers.get(headerName);
  }

  @Override
  public Collection<String> getHeaderValues(String headerName) {
    return headers.getAll(headerName);
  }

  @Override
  public CaseInsensitiveMultiMap getHeaders() {
    return headers.toImmutableMultiMap();
  }

  @Override
  public String getHeaderValueIgnoreCase(String headerName) {
    String lowerCaseValue = getHeaderValue(headerName.toLowerCase());
    return lowerCaseValue != null ? lowerCaseValue : getHeaderValue(headerName);
  }

  @Override
  public Collection<String> getHeaderValuesIgnoreCase(String headerName) {
    Collection<String> lowerCaseValue = getHeaderValues(headerName.toLowerCase());
    return lowerCaseValue != null ? lowerCaseValue : getHeaderValues(headerName);
  }
}
