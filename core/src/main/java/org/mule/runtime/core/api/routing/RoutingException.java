/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.routing;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.Message;

/**
 * <code>RoutingException</code> is a base class for all routing exceptions. Routing exceptions are only thrown for
 * DefaultInboundRouterCollection and DefaultOutboundRouterCollection and deriving types. Mule itself does not throw routing
 * exceptions when routing internal events.
 */
public class RoutingException extends MessagingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 2478458847072048645L;

  protected final transient MessageProcessor route;

  public RoutingException(MuleEvent event, MessageProcessor route) {
    super(generateMessage(null, route), event, route);
    this.route = route;
  }

  public RoutingException(MuleEvent event, MessageProcessor route, Throwable cause) {
    super(generateMessage(null, route), event, cause, route);
    this.route = route;
  }

  public RoutingException(Message message, MuleEvent event, MessageProcessor route) {
    super(generateMessage(message, route), event, route);
    this.route = route;
  }

  public RoutingException(Message message, MuleEvent event, MessageProcessor route, Throwable cause) {
    super(generateMessage(message, route), event, cause, route);
    this.route = route;
  }

  public MessageProcessor getRoute() {
    return route;
  }

  private static Message generateMessage(Message message, MessageProcessor target) {
    Message m = CoreMessages.failedToRouterViaEndpoint(target);
    if (message != null) {
      message.setNextMessage(m);
      return message;
    } else {
      return m;
    }
  }
}
