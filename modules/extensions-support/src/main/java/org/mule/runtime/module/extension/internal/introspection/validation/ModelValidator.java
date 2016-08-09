/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;

/**
 * Validates that a constructed model is legal, meaning it's consistent and doesn't violate any restriction.
 *
 * @since 4.0
 */
public interface ModelValidator {

  /**
   * Validates the given {@code model}
   *
   * @param model a {@link ExtensionModel}
   * @throws IllegalModelDefinitionException if the model is illegal
   */
  void validate(ExtensionModel model) throws IllegalModelDefinitionException;
}
