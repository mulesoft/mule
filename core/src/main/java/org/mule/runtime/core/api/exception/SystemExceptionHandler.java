/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.component.location.ComponentLocation;

/**
 * Take some action when a system exception has occurred (i.e., there was no message in play when the exception occurred).
 */
public interface SystemExceptionHandler {

  /**
   * Take some action when a system exception has occurred (i.e., there was no message in play when the exception occurred).
   *
   * @param exception      which occurred
   * @param rollbackMethod will be called if transactions are not used in order to achieve atomic message delivery
   */
  void handleException(Exception exception, RollbackSourceCallback rollbackMethod);

  /**
   * Take some action when a system exception has occurred (i.e., there was no message in play when the exception occurred).
   *
   * @param exception which occurred
   */
  void handleException(Exception exception);

  /**
   * Take some action when a system exception has occurred (i.e., there was no message in play when the exception occurred).
   *
   * @param exception         which occurred
   * @param componentLocation the {@link ComponentLocation} which produced the exception
   *
   * @since 4.4.0
   */
  default void handleException(Exception exception, ComponentLocation componentLocation) {
    handleException(exception);
  }
}


