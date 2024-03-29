/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Stoppable;

/** <code>DisposeException</code> TODO (document class) */

public final class StopException extends LifecycleException {

  /** Serial version */
  private static final long serialVersionUID = 1714192220605243680L;

  /**
   * @param message   the exception message
   * @param component the object that failed during a lifecycle method call
   */
  public StopException(I18nMessage message, Stoppable component) {
    super(message, component);
  }

  /**
   * @param message   the exception message
   * @param cause     the exception that cause this exception to be thrown
   * @param component the object that failed during a lifecycle method call
   */
  public StopException(I18nMessage message, Throwable cause, Stoppable component) {
    super(message, cause, component);
  }

  /**
   * @param cause     the exception that cause this exception to be thrown
   * @param component the object that failed during a lifecycle method call
   */
  public StopException(Throwable cause, Stoppable component) {
    super(cause, component);
  }
}
