/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;
import static org.mule.runtime.core.config.i18n.CoreMessages.authFailedForUser;
import static org.mule.runtime.core.config.i18n.CoreMessages.authSetButNoContext;
import static org.mule.runtime.core.config.i18n.CoreMessages.authorizationDeniedOnEndpoint;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.Message;

/**
 * <code>NotPermittedException</code> is thrown if the user isn't authorized to perform an action.
 */
public class NotPermittedException extends SecurityException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -6664384216189042673L;

  public NotPermittedException(Message message) {
    super(message, getCurrentEvent());
  }

  public NotPermittedException(Message message, Throwable cause, MessageProcessor failingMessageProcessor) {
    super(message, getCurrentEvent(), cause, failingMessageProcessor);
  }

  public NotPermittedException(Message message, MuleEvent event) {
    super(message, event);
  }

  public NotPermittedException(Message message, MuleEvent event, Throwable cause, MessageProcessor failingMessageProcessor) {
    super(message, event, cause, failingMessageProcessor);
  }

  public NotPermittedException(MuleEvent event, SecurityContext context, SecurityFilter filter) {
    super(constructMessage(context, event.getContext().getOriginatingConnectorName(), filter), event);
  }

  private static Message constructMessage(SecurityContext context,
                                          String originatingConnectorName,
                                          SecurityFilter filter) {
    Message m;
    if (context == null) {
      m = authSetButNoContext(filter.getClass().getName());
    } else {
      m = authFailedForUser(context.getAuthentication().getPrincipal());
    }
    m.setNextMessage(authorizationDeniedOnEndpoint(originatingConnectorName));
    return m;
  }
}
