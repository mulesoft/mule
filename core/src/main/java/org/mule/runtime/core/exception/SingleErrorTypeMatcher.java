/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.ErrorTypeRepository.ANY_ERROR_TYPE;
import org.mule.runtime.api.message.ErrorType;

public class SingleErrorTypeMatcher implements ErrorTypeMatcher {

  private final ErrorType errorType;

  public SingleErrorTypeMatcher(ErrorType errorType) {
    this.errorType = errorType;
  }

  @Override
  public boolean match(ErrorType errorType) {
    return ANY_ERROR_TYPE.equals(this.errorType) || this.errorType.equals(errorType) || isChild(errorType);
  }

  private boolean isChild(ErrorType errorType) {
    ErrorType parentErrorType = errorType.getParentErrorType();
    return parentErrorType != null && !ANY_ERROR_TYPE.equals(parentErrorType) && this.match(parentErrorType);
  }
}
