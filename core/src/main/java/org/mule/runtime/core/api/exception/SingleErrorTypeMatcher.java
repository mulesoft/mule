/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.message.ErrorType;

public final class SingleErrorTypeMatcher extends org.mule.runtime.api.exception.matcher.SingleErrorTypeMatcher
    implements ErrorTypeMatcher {

  public SingleErrorTypeMatcher(ErrorType errorType) {
    super(errorType);
  }

}
