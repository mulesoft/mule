/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
