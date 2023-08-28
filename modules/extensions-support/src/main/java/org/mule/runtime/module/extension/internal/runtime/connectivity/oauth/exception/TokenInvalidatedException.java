/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
