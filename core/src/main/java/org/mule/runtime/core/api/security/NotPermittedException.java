/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.mule.runtime.core.config.i18n.CoreMessages.authFailedForUser;
import static org.mule.runtime.core.config.i18n.CoreMessages.authSetButNoContext;
import static org.mule.runtime.core.config.i18n.CoreMessages.authorizationDeniedOnEndpoint;
import static org.mule.runtime.core.message.DefaultEventBuilder.MuleEventImplementation.getCurrentEvent;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.I18nMessage;

/**
 * <code>NotPermittedException</code> is thrown if the user isn't authorized to perform an action.
 */
public class NotPermittedException extends SecurityException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -6664384216189042673L;

  public NotPermittedException(I18nMessage message) {
    super(message, getCurrentEvent());
  }

  public NotPermittedException(I18nMessage message, Throwable cause, Processor failingMessageProcessor) {
    super(message, getCurrentEvent(), cause, failingMessageProcessor);
  }

  public NotPermittedException(I18nMessage message, Event event) {
    super(message, event);
  }

  public NotPermittedException(I18nMessage message, Event event, Throwable cause, Processor failingMessageProcessor) {
    super(message, event, cause, failingMessageProcessor);
  }

  public NotPermittedException(Event event, SecurityContext context, SecurityFilter filter) {
    super(constructMessage(context, event.getContext().getOriginatingConnectorName(), filter), event);
  }

  private static I18nMessage constructMessage(SecurityContext context,
                                              String originatingConnectorName,
                                              SecurityFilter filter) {
    I18nMessage m;
    if (context == null) {
      m = authSetButNoContext(filter.getClass().getName());
    } else {
      m = authFailedForUser(context.getAuthentication().getPrincipal());
    }
    m.setNextMessage(authorizationDeniedOnEndpoint(originatingConnectorName));
    return m;
  }
}
