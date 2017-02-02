/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.client;

import org.mule.runtime.core.api.MessageExchangePattern;
import org.mule.runtime.api.util.Preconditions;

/**
 * Cache key used to locate an object in a map based on an URL and operation options.
 *
 * @param <OptionsType> the expected type for the operation options
 */

public class RequestCacheKey<OptionsType extends OperationOptions> {

  private final String url;
  private final OptionsType operationOptions;
  private final MessageExchangePattern exchangePattern;

  public RequestCacheKey(final String url, final OptionsType operationOptions, final MessageExchangePattern exchangePattern) {
    Preconditions.checkArgument(url != null, "URL cannot be null");
    Preconditions.checkArgument(operationOptions != null, "Operation options cannot be null");
    Preconditions.checkArgument(exchangePattern != null, "Exchange pattern cannot be null");
    this.url = url;
    this.operationOptions = operationOptions;
    this.exchangePattern = exchangePattern;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RequestCacheKey)) {
      return false;
    }

    RequestCacheKey that = (RequestCacheKey) o;

    if (!operationOptions.equals(that.operationOptions)) {
      return false;
    }
    if (!url.equals(that.url)) {
      return false;
    }
    if (this.exchangePattern != that.exchangePattern) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = url.hashCode();
    result = 31 * result + operationOptions.hashCode();
    result = 31 * result + exchangePattern.hashCode();
    return result;
  }

  public String getUrl() {
    return url;
  }

  public OptionsType getOperationOptions() {
    return operationOptions;
  }

  public MessageExchangePattern getExchangePattern() {
    return exchangePattern;
  }
}
