/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
