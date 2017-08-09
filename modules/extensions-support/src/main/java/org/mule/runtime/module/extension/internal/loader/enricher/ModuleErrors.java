/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

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
