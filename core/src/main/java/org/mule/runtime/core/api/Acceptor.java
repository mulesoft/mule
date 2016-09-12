/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

/**
 * Provides capability to only accept handling certain MuleEvents.
 */
public interface Acceptor {

  /**
   * @param event {@link Event} to route through exception handler
   * @return true if this {@link org.mule.runtime.core.api.exception.MessagingExceptionHandler} should handler exception false
   *         otherwise
   */
  boolean accept(Event event);

  /**
   * @return true if accepts any message, false otherwise.
   */
  boolean acceptsAll();
}
