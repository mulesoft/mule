/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.slf4j.Logger;

/**
 * Utility class to log a warning message only once during the lifecycle of a component. The {@link #warn()} message will log that
 * message only once, in a thread-safe manner. This component is conceived with the intent of allowing deprecated components to
 * warn that they should not be used anymore.
 *
 * @since 3.6.0
 */
public class OneTimeWarning {

  private final Logger logger;
  private final String message;
  private boolean warned = false;
  private Delegate delegate = new FirstTimeDelegate();

  public OneTimeWarning(Logger logger, String message) {
    this.logger = logger;
    this.message = message;
  }

  /**
   * Fire one time warning.
   */
  public void warn() {
    delegate.warn();
  }

  /**
   * Fire one time warning using arguments passing arguments to logger
   *
   * @param args arguments to pass to logger along with message defined when this instance was created.
   */
  public void warn(Object... args) {
    delegate.warn(args);
  }

  private interface Delegate {

    void warn();

    void warn(Object... args);
  }

  private class FirstTimeDelegate implements Delegate {

    @Override
    public synchronized void warn() {
      warn(null);
    }

    @Override
    public synchronized void warn(Object... args) {
      if (warned == false) {
        if (args != null) {
          logger.warn(message, args);
        } else {
          logger.warn(message);
        }
        warned = true;
        delegate = new NoOpDelegate();
      }
    }
  }

  private class NoOpDelegate implements Delegate {

    @Override
    public void warn() {}

    @Override
    public void warn(Object... args) {}
  }
}
