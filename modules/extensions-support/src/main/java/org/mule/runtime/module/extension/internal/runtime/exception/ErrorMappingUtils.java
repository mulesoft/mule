/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.privileged.util.ModelPropertiesDeclarationUtils.isNoErrorMapping;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utilities for dealing with {@code error-mapping} in operations.
 *
 * @since 4.4
 */
public final class ErrorMappingUtils {

  private ErrorMappingUtils() {
    // Nothing to do
  }

  /**
   * For the given AST node representing an operation, execute the given {@code action} for each error mapping it has.
   *
   * @param operation the operation from which to iterate the error mappings.
   * @param action    what is executed for every error mapping.
   */
  public static void forEachErrorMappingDo(ComponentAst operation, Consumer<List<ErrorMapping>> action) {
    operation.getModel(OperationModel.class).ifPresent(opModel -> {
      if (!isNoErrorMapping(opModel)) {
        final ComponentParameterAst errorMappingsParam = operation.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME);
        if (errorMappingsParam != null) {
          errorMappingsParam.<List<ErrorMapping>>getValue().applyRight(action);
        }
      }
    });
  }
}
