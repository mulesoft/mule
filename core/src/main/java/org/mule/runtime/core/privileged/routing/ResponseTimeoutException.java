/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.routing;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * <code>ResponseTimeoutException</code> is thrown when a response is not received in a given timeout in the Response Router.
 * 
 */
public final class ResponseTimeoutException extends RoutingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 6882278747922113242L;

  public ResponseTimeoutException(I18nMessage message, Processor target) {
    super(message, target);
  }

  public ResponseTimeoutException(I18nMessage message, Processor target, Throwable cause) {
    super(message, target, cause);
  }
}
