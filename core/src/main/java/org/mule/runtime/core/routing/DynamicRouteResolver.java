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
 * SPI for dynamic route resolvers
 */
public interface DynamicRouteResolver {

  /**
   * Return a list of {@link MessageProcessor} to route the message.
   *
   * @param event the event holding the message to route
   * @return a list of {@link MessageProcessor} to which the message will be routed to
   * @throws MessagingException
   */
  List<MessageProcessor> resolveRoutes(MuleEvent event) throws MessagingException;
}
