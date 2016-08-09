/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transport;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.routing.filter.Filter;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface LegacyOutboundEndpoint {

  /**
   * The filter to apply to incoming messages. Only applies when the endpoint endpointUri is a receiver
   *
   * @return the Filter to use or null if one is not set
   */
  Filter getFilter();

  MessageExchangePattern getExchangePattern();

  default boolean filterAccepts(MuleMessage message) {
    return getFilter() == null || (getFilter() != null && getFilter().accept(message));
  }

  default boolean mayReturnVoidEvent() {
    MessageExchangePattern exchangePattern = getExchangePattern();
    return exchangePattern == null ? true : !exchangePattern.hasResponse();
  }

}
