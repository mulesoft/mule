/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.domain;

import org.mule.service.http.api.domain.HttpMessage;

import java.util.Collection;

/**
 * Base implementation for {@HttpMessage} that handles ignore case header operations. The lower case version is attempted first
 * since it's the Grizzly default.
 *
 * @since 3.9
 */
public abstract class BaseHttpMessage implements HttpMessage {

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
