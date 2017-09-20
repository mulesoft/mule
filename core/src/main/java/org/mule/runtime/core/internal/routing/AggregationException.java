/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.routing.RoutingException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * TODO document
 *
 */
public class AggregationException extends RoutingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 1276049971165761454L;

  private EventGroup eventGroup = null;

  public AggregationException(EventGroup eventGroup, Processor endpoint) {
    super(endpoint);
    this.eventGroup = eventGroup;
  }

  public AggregationException(EventGroup eventGroup, Processor endpoint, Throwable cause) {
    super(endpoint, cause);
    this.eventGroup = eventGroup;
  }

  public AggregationException(I18nMessage message, EventGroup eventGroup, Processor endpoint) {
    super(message, endpoint);
    this.eventGroup = eventGroup;
  }

  public AggregationException(I18nMessage message, EventGroup eventGroup, Processor endpoint, Throwable cause) {
    super(message, endpoint, cause);
    this.eventGroup = eventGroup;
  }

  public EventGroup getEventGroup() {
    return eventGroup;
  }
}
