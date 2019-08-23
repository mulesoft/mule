/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.exception;

import static org.mule.runtime.extension.api.error.MuleErrors.CONNECTIVITY;

import org.mule.runtime.extension.api.exception.ModuleException;

public class TokenInvalidatedException extends ModuleException {

  public TokenInvalidatedException(String message) {
    super(message, CONNECTIVITY);
  }

  public TokenInvalidatedException(String message, Throwable cause) {
    super(message, CONNECTIVITY, cause);
  }
}
