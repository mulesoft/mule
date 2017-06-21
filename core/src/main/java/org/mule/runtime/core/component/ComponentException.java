/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.component;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * <code>ComponentException</code> should be thrown when some action on a component fails, such as starting or stopping
 */
// @ThreadSafe
public class ComponentException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 56178344205041600L;

  private final transient Component component;

  public ComponentException(I18nMessage message, Component component) {
    super(generateMessage(message, component));
    this.component = component;
  }

  public ComponentException(I18nMessage message, Component component, Throwable cause) {
    super(generateMessage(message, component), cause);
    this.component = component;
  }

  public ComponentException(Component component, Throwable cause) {
    super(generateMessage(null, component), cause);
    this.component = component;
  }

  public Component getComponent() {
    return component;
  }

  @Override
  protected void setMessage(I18nMessage message) {
    super.setMessage(message.getMessage());
  }

  private static I18nMessage generateMessage(I18nMessage previousMessage, Component component) {
    I18nMessage returnMessage = CoreMessages.componentCausedErrorIs(component);
    if (previousMessage != null) {
      previousMessage.setNextMessage(returnMessage);
      return previousMessage;
    } else {
      return returnMessage;
    }
  }
}
