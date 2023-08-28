/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Indicates that all the memory that was assigned for streaming operations has already been consumed
 *
 * @since 4.0
 */
public class MaxStreamingMemoryExceededException extends MuleRuntimeException {

  /**
   * Indicates that all the memory that was assigned for streaming operations has already been consumed
   *
   * @param message
   */
  public MaxStreamingMemoryExceededException(I18nMessage message) {
    super(message);
  }

}
