/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Thrown by the ServerNotification Manager if unrecognised listeners or events are passed to the manager
 */
public class NotificationException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -5998352122311445746L;

  /**
   * @param message the exception message
   */
  public NotificationException(I18nMessage message) {
    super(message);
  }

  /**
   * @param message the exception message
   * @param cause the exception that cause this exception to be thrown
   */
  public NotificationException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
