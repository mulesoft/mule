/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.routing.filter;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

/**
 * The <code>Filter</code> interface allows MuleMessage filtering.
 */

public interface Filter {

  /**
   * Checks a given message against this filter.
   * 
   * @param message a non null message to filter.
   * @return <code>true</code> if the message matches the filter TODO MULE-9341 Remove Filters that are not needed. This method
   *         will be removed when filters are cleaned up.
   */
  @Deprecated
  boolean accept(MuleMessage message);

  /**
   * Checks a given event against this filter.
   *
   * @param event a non null event to filter.
   * @return <code>true</code> if the event matches the filter
   */
  default boolean accept(MuleEvent event) {
    return accept(event.getMessage());
  }

}
