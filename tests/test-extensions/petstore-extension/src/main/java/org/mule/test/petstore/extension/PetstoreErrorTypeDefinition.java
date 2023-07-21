/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

public enum PetstoreErrorTypeDefinition implements ErrorTypeDefinition<PetstoreErrorTypeDefinition> {

  CONNECTIVITY(MuleErrors.CONNECTIVITY), PET_STORE_CONNECTIVITY(CONNECTIVITY), PET_ERROR;

  private ErrorTypeDefinition<? extends Enum<?>> parentError;

  private PetstoreErrorTypeDefinition(ErrorTypeDefinition<? extends Enum<?>> parentError) {
    this.parentError = parentError;
  }

  private PetstoreErrorTypeDefinition() {}

  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return Optional.ofNullable(this.parentError);
  }
}
