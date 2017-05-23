/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Optional;

public enum DrStrangeErrorTypeDefinition implements ErrorTypeDefinition<DrStrangeErrorTypeDefinition> {

  CUSTOM_ERROR;

  private ErrorTypeDefinition<?> parentErrorType;

  DrStrangeErrorTypeDefinition(ErrorTypeDefinition<?> parentErrorType) {
    this.parentErrorType = parentErrorType;
  }



  DrStrangeErrorTypeDefinition() {}

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return Optional.ofNullable(parentErrorType);
  }
}
