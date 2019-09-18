/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.message.ErrorType;

public interface ErrorTypeMatcherFactory {

  /**
   * Creates an apropriate {@link ErrorTypeMatcher} for the corresponding {@link ErrorType}
   *
   * @param errorType Error type which the output matcher should match.
   * @return An {@link ErrorTypeMatcher} which can match the input error type.
   */
  ErrorTypeMatcher create(ErrorType errorType);
}
