/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.exception;

import static org.mule.test.heisenberg.extension.HeisenbergErrors.VALIDATION;
import org.mule.sdk.api.annotation.error.ErrorTypeProvider;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.Collections;
import java.util.Set;

public class ValidationErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return Collections.singleton(VALIDATION);
  }
}
