/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import org.mule.runtime.api.exception.MuleException;

/**
 * Phase for validation of the incoming message.
 *
 * This template allows to validate a message and discard it in case is invalid.
 */
public interface ValidationPhaseTemplate extends MessageProcessTemplate {

  /**
   * Validates the message content.
   *
   * In case that the message is not valid then {@link #discardInvalidMessage()} will be executed so the implementation can save
   * the reason why the message is invalid to report why the message has been discarded when {@link #discardInvalidMessage()} is
   * called
   *
   * @return false if the message is invalid, true otherwise
   */
  boolean validateMessage();

  /**
   * Discards the message because the validation failed
   *
   * @throws MuleException
   */
  void discardInvalidMessage() throws MuleException;
}
