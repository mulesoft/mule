/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * {@code MuleFatalException} Is the runtime exception used to wrap fatal errors such as {@code VirtualMachineError}. This
 * is to avoid Reactor intercepting them in a stream and propagating outside to arbitrary threads.
 *
 * @since 4.0
 */
public final class MuleFatalException extends MuleRuntimeException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -3729492434108165916L;

  /**
   * @param message the exception message
   */
  public MuleFatalException(I18nMessage message) {
    super(message);
  }

  /**
   * @param message the exception message
   * @param cause the exception that triggered this exception
   */
  public MuleFatalException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param cause the exception that triggered this exception
   */
  public MuleFatalException(Throwable cause) {
    super(cause);
  }
}
