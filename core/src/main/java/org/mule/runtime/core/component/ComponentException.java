/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.component;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.Message;

/**
 * <code>ComponentException</code> should be thrown when some action on a component fails, such as starting or stopping
 */
// @ThreadSafe
public class ComponentException extends MessagingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 56178344205041600L;

  private final transient Component component;

  public ComponentException(Message message, MuleEvent muleMessage, Component component) {
    super(generateMessage(message, component), muleMessage, component);
    this.component = component;
  }

  public ComponentException(Message message, MuleEvent event, Component component, Throwable cause) {
    super(generateMessage(message, component), event, cause, component);
    this.component = component;
  }

  public ComponentException(MuleEvent message, Component component, Throwable cause) {
    super(generateMessage(null, component), message, cause, component);
    this.component = component;
  }

  public Component getComponent() {
    return component;
  }

  private static Message generateMessage(Message previousMessage, Component component) {
    Message returnMessage = CoreMessages.componentCausedErrorIs(component);
    if (previousMessage != null) {
      previousMessage.setNextMessage(returnMessage);
      return previousMessage;
    } else {
      return returnMessage;
    }
  }
}
