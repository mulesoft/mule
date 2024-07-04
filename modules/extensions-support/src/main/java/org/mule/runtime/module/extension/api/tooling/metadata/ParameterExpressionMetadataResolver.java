/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.tooling.metadata;

import static java.util.Optional.empty;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.internal.metadata.ComponentParameterizationExpressionMetadataResolver;

import java.util.Optional;

/**
 * Allows for resolving actual input metadata for parameters that have been parameterized with expressions.
 * <p>
 * <b>NOTE:</b> Experimental feature. Backwards compatibility not guaranteed.
 *
 * @since 4.8
 */
@Experimental
@NoImplement
public interface ParameterExpressionMetadataResolver {

  /**
   * Constructs a {@link ParameterExpressionMetadataResolver} from a {@link ComponentParameterization}.
   *
   * @param parameterization                  The {@link ComponentParameterization}.
   * @param typeBindings                      The {@link TypeBindings} in context for resolving references to bindings.
   * @param expressionManager                 The {@link ExpressionManager} for determining if something is an expression.
   * @param expressionLanguageMetadataService The {@link ExpressionLanguageMetadataService} for actually resolving the
   *                                          expression's output type.
   * @return A {@link ParameterExpressionMetadataResolver} bound to the given {@link ComponentParameterization} and
   *         {@link TypeBindings}.
   */
  static ParameterExpressionMetadataResolver fromComponentParameterization(ComponentParameterization<?> parameterization,
                                                                           TypeBindings typeBindings,
                                                                           ExpressionManager expressionManager,
                                                                           ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    return new ComponentParameterizationExpressionMetadataResolver(parameterization, typeBindings, expressionManager,
                                                                   expressionLanguageMetadataService);
  }

  /**
   * @return A {@link ParameterExpressionMetadataResolver} that always returns no metadata.
   */
  static ParameterExpressionMetadataResolver noOp() {
    return (parameterModel) -> empty();
  }

  /**
   * @param parameterName The name of the parameter.
   * @return The actual metadata of the expression output, only if the parameter was given an expression.
   */
  Optional<MetadataResult<MetadataType>> getActualInputMetadataIfExpression(String parameterName);
}
