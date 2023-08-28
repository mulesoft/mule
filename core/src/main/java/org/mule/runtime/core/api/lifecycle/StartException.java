/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;

/** <code>DisposeException</code> TODO (document class) */

public final class StartException extends LifecycleException {

  /** Serial version */
  private static final long serialVersionUID = 1714192220605243680L;

  /**
   * @param message   the exception message
   * @param component the object that failed during a lifecycle method call
   */
  public StartException(I18nMessage message, Startable component) {
    super(message, component);
  }

  /**
   * @param message   the exception message
   * @param cause     the exception that cause this exception to be thrown
   * @param component the object that failed during a lifecycle method call
   */
  public StartException(I18nMessage message, Throwable cause, Startable component) {
    super(message, cause, component);
  }

  /**
   * @param cause     the exception that cause this exception to be thrown
   * @param component the object that failed during a lifecycle method call
   */
  public StartException(Throwable cause, Startable component) {
    super(cause, component);
  }
}
