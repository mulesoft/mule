/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;

import org.mule.runtime.api.meta.model.operation.ErrorMappings.ErrorMapping;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @since 4.4
 */
public final class ErrorMappingUtils {

  private ErrorMappingUtils() {
    // Nothing to do
  }

  public static void doForErrorMappings(ComponentAst operation, Consumer<List<ErrorMapping>> action) {
    if (operation.getModel(OperationModel.class).isPresent()) {
      final ComponentParameterAst errorMappingsParam = operation.getParameter(ERROR_MAPPINGS_PARAMETER_NAME);
      if (errorMappingsParam != null) {
        errorMappingsParam.<List<ErrorMapping>>getValue().applyRight(action);
      }
    }
  }
}
