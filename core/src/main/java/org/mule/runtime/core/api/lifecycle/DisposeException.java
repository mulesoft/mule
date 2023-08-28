/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.LifecycleException;

/** <code>DisposeException</code> TODO (document class) */

public final class DisposeException extends LifecycleException {

  /** Serial version */
  private static final long serialVersionUID = 1714192290605243680L;

  /**
   * @param message   the exception message
   * @param component the object that failed during a lifecycle method call
   */
  public DisposeException(I18nMessage message, Disposable component) {
    super(message, component);
  }

  /**
   * @param message   the exception message
   * @param cause     the exception that cause this exception to be thrown
   * @param component the object that failed during a lifecycle method call
   */
  public DisposeException(I18nMessage message, Throwable cause, Disposable component) {
    super(message, cause, component);
  }

  /**
   * @param cause     the exception that cause this exception to be thrown
   * @param component the object that failed during a lifecycle method call
   */
  public DisposeException(Throwable cause, Disposable component) {
    super(cause, component);
  }
}
