/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.api.exception;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.error.MuleErrors.SECURITY;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Optional;

/**
 * Errors that can happen during an OAuth dance.
 *
 * @since 4.0
 */
public enum OAuthErrors implements ErrorTypeDefinition<OAuthErrors> {

  OAUTH_ERROR(SECURITY), TOKEN_NOT_FOUND(OAUTH_ERROR), TOKEN_URL_FAIL(OAUTH_ERROR);

  private ErrorTypeDefinition<?> parentErrortype;

  private OAuthErrors(ErrorTypeDefinition parentErrorType) {
    this.parentErrortype = parentErrorType;
  }

  private OAuthErrors() {}

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parentErrortype);
  }
}
