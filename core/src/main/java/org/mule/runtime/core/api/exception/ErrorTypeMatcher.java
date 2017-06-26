/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.message.ErrorType;

/**
 * Decides whether an error type is acceptable.
 *
 * @since 4.0
 */
public interface ErrorTypeMatcher {

  /**
   * @param errorType the {@link ErrorType} to check
   * @return {@code true} if a match is possible, {@code false} otherwise
   */
  boolean match(ErrorType errorType);

}
