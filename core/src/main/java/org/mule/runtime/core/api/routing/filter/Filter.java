/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.routing.filter;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;

/**
 * The <code>Filter</code> interface allows Message filtering.
 */

public interface Filter {

  /**
   * Checks a given message against this filter.
   * <p>
   * TODO MULE-9142 See how this API can be improved to not need the builder.
   * 
   * @param message a non null message to filter.
   * @param builder an event builder in case the filter needs to make changes to the event.
   * @return <code>true</code> if the message matches the filter TODO MULE-9341 Remove Filters that are not needed. This method
   *         will be removed when filters are cleaned up.
   */
  @Deprecated
  boolean accept(Message message, Event.Builder builder);

  /**
   * Checks a given event against this filter.
   * <p>
   * TODO MULE-9142 See how this API can be improved to not need the builder.
   *
   * @param event a non null event to filter.
   * @param builder an event builder in case the filter needs to make changes to the event.
   * @return <code>true</code> if the event matches the filter
   */
  default boolean accept(Event event, Event.Builder builder) {
    return accept(event.getMessage(), builder);
  }

}
