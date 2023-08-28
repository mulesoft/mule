/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
