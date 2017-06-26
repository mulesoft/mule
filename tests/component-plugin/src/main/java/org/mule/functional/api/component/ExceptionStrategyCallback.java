/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.component;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;

/**
 * Provides a way to manage exceptions during integration tests
 */
public interface ExceptionStrategyCallback {

  /**
   * Manages a messaging exception
   *
   * @param exception exception that was thrown during flow execution.
   * @param event     event that was begin processed when the exception was thrown.
   * @param delegate  exception handler defined in the flow.
   * @return the result of the managed exception.
   *
   * @since 4.0
   */
  Event handleException(MessagingException exception, Event event, MessagingExceptionHandler delegate);
}
