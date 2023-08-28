/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension;

import static java.util.Optional.ofNullable;

import org.mule.sdk.api.error.ErrorTypeDefinition;
import org.mule.sdk.api.error.MuleErrors;

import java.util.Optional;

public enum HeisenbergErrors implements ErrorTypeDefinition<HeisenbergErrors> {

  CONNECTIVITY(MuleErrors.CONNECTIVITY), OAUTH2(HeisenbergErrors.CONNECTIVITY), HEALTH(HeisenbergErrors.CONNECTIVITY), VALIDATION(
      MuleErrors.VALIDATION);

  private ErrorTypeDefinition<?> parentErrortype;

  HeisenbergErrors(ErrorTypeDefinition parentErrorType) {
    this.parentErrortype = parentErrorType;
  }

  HeisenbergErrors() {}

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parentErrortype);
  }
}
