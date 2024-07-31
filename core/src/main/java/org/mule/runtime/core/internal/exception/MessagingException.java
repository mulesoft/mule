/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.core.api.event.CoreEvent;



/**
 * <code>MessagingException</code> is a general message exception thrown when errors specific to Message processing occur..
 */
public class MessagingException extends org.mule.runtime.core.privileged.exception.MessagingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 6941498759267936651L;

  public MessagingException(I18nMessage message, CoreEvent event) {
    super(message, event);
  }

  public MessagingException(I18nMessage message, CoreEvent event, Component failingComponent) {
    super(message, event, failingComponent);
  }

  public MessagingException(I18nMessage message, CoreEvent event, Throwable cause) {
    super(message, event, cause);
  }

  public MessagingException(I18nMessage message, CoreEvent event, Throwable cause, Component failingComponent) {
    super(message, event, cause, failingComponent);
  }

  public MessagingException(CoreEvent event, Throwable cause) {
    super(event, cause);
  }

  public MessagingException(CoreEvent event, MessagingException original) {
    super(event, original);
  }

  public MessagingException(CoreEvent event, Throwable cause, Component failingComponent) {
    super(event, cause, failingComponent);
  }
}
