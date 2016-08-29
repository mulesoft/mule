/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.util.List;

/**
 * Defines an strategy to route a {@link org.mule.runtime.core.api.MuleEvent} through a set of
 * {@link org.mule.runtime.core.api.processor.MessageProcessor}
 */
public interface RoutingStrategy {

  /**
   * Routes {@link MuleEvent} through a set of {@link MessageProcessor}
   *
   * @param event
   * @param messageProcessors
   * @return
   */
  MuleEvent route(MuleEvent event, List<MessageProcessor> messageProcessors) throws MessagingException;
}
