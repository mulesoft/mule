/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.connector;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.routing.RoutingException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * <code>DispatchException</code> is thrown when a connector operation dispatcher fails to send, dispatch or receive a message.
 */
public class DispatchException extends RoutingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -8204621943732496606L;

  public DispatchException(Processor target) {
    super(target);
  }

  public DispatchException(Processor target, Throwable cause) {
    super(target, cause);
  }

  public DispatchException(I18nMessage message, Processor target) {
    super(message, target);
  }

  public DispatchException(I18nMessage message, Processor target, Throwable cause) {
    super(message, target, cause);
  }
}
