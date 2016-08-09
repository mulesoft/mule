/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.client;

import org.mule.runtime.core.api.client.OperationOptions;

/**
 * Default implementation for {@link org.mule.runtime.core.api.client.OperationOptions}
 */
public class SimpleOptions implements OperationOptions {

  private final Long responseTimeout;
  private final boolean outbound;

  public SimpleOptions(Long responseTimeout, boolean outbound) {
    this.responseTimeout = responseTimeout;
    this.outbound = outbound;
  }

  @Override
  public Long getResponseTimeout() {
    return responseTimeout;
  }

  public boolean isOutbound() {
    return outbound;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleOptions)) {
      return false;
    }

    SimpleOptions that = (SimpleOptions) o;

    if (responseTimeout == null ? that.responseTimeout != null : !responseTimeout.equals(that.responseTimeout)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    final Long responseTimeout = getResponseTimeout();
    return responseTimeout != null ? responseTimeout.hashCode() : 0;
  }
}
