/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.api.tooling.metadata.ParameterExpressionMetadataResolver;

import java.util.Optional;

/**
 * A {@link ParameterExpressionMetadataResolver} that is bound to a {@link ComponentParameterization}.
 */
public class ComponentParameterizationExpressionMetadataResolver extends AbstractParameterExpressionMetadataResolver {

  private final ComponentParameterization<?> parameterization;

  public ComponentParameterizationExpressionMetadataResolver(ComponentParameterization<?> parameterization,
                                                             TypeBindings typeBindings,
                                                             ExpressionManager expressionManager,
                                                             ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    super(typeBindings, expressionManager, expressionLanguageMetadataService);
    this.parameterization = parameterization;
  }

  @Override
  protected Optional<Pair<ParameterModel, Object>> getParameter(String parameterName) {
    return parameterization.getParameters()
        .entrySet().stream()
        .filter(e -> e.getKey().getSecond().getName().equals(parameterName))
        .findFirst()
        .map(e -> new Pair<>(e.getKey().getSecond(), e.getValue()));
  }
}
