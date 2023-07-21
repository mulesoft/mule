/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.error;

import org.mule.sdk.api.error.ErrorTypeDefinition;
import org.mule.sdk.api.error.MuleErrors;

import java.util.Optional;

public enum ModuleErrors implements ErrorTypeDefinition<ModuleErrors> {

  CONNECTIVITY {

    @Override
    public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
      return Optional.of(MuleErrors.CONNECTIVITY);
    }
  },
  RETRY_EXHAUSTED {

    @Override
    public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
      return Optional.of(MuleErrors.RETRY_EXHAUSTED);
    }
  }
}
