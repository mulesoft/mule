/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;

public class OperationDoesNotHaveCoreRaiseError extends MuleSdkOperationDoesNotHaveForbiddenComponents {

  private static final String CORE_RAISE_ERROR_ELEMENT = "raise-error";
  private static final ComponentIdentifier CORE_RAISE_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(CORE_RAISE_ERROR_ELEMENT).build();

  @Override
  protected ComponentIdentifier forbiddenComponentIdentifier() {
    return CORE_RAISE_ERROR_IDENTIFIER;
  }
}
