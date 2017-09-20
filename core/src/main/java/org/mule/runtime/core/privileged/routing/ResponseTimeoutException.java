/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * <code>ResponseTimeoutException</code> is thrown when a response is not received in a given timeout in the Response Router.
 * 
 */
public class ResponseTimeoutException extends RoutingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 6882278747922113240L;

  public ResponseTimeoutException(I18nMessage message, Processor target) {
    super(message, target);
  }

  public ResponseTimeoutException(I18nMessage message, Processor target, Throwable cause) {
    super(message, target, cause);
  }
}
