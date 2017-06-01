/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Indicates that all the memory that was assigned for streaming operations has already been
 * consumed
 *
 * @since 4.0
 */
public class MaxStreamingMemoryExceededException extends MuleRuntimeException {

  /**
   * {@inheritDoc}
   */
  public MaxStreamingMemoryExceededException(I18nMessage message) {
    super(message);
  }

}
