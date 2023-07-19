/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.runtime.extension.internal.property.NoErrorMappingModelProperty;

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
      if (!opModel.getModelProperty(NoErrorMappingModelProperty.class).isPresent()) {
        final ComponentParameterAst errorMappingsParam = operation.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME);
        if (errorMappingsParam != null) {
          errorMappingsParam.<List<ErrorMapping>>getValue().applyRight(action);
        }
      }
    });
  }
}
