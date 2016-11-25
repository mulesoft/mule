/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

public enum HeisenbergErrors implements ErrorTypeDefinition<HeisenbergErrors> {
  HEALTH {

    @Override
    public Optional<ErrorTypeDefinition<?>> getParent() {
      return Optional.of(CONNECTIVITY);
    }
  },
  CONNECTIVITY {

    @Override
    public Optional<ErrorTypeDefinition<?>> getParent() {
      return Optional.of(MuleErrors.CONNECTIVITY);
    }
  },
  OAUTH2 {

    @Override
    public Optional<ErrorTypeDefinition<?>> getParent() {
      return Optional.of(HeisenbergErrors.CONNECTIVITY);
    }
  }
}
