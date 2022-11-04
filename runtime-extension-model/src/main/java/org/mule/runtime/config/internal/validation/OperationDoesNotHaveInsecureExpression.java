/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.internal.util.ExpressionUtils.isExpression;

import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.el.validation.ScopePhaseValidationMessages;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class OperationDoesNotHaveInsecureExpression extends OperationValidation {

  private final ExpressionLanguage expressionLanguage;

  public OperationDoesNotHaveInsecureExpression(ExpressionLanguage el) {
    this.expressionLanguage = el;
  }

  @Override
  public String getName() {
    return "Operation Doesn't have insecure expressions";
  }

  @Override
  public String getDescription() {
    return "The operation does not have a function or operation marked as insecure within itself";
  }

  private boolean isInsecure(String expression) {
    if (!isExpression(expression)) {
      return false;
    }
    ScopePhaseValidationMessages result =
        expressionLanguage.collectScopePhaseValidationMessages(expression, "tuvieja", TypeBindings.builder().build());
    return false;
  }

  /**
   * @return All the (raw) expressions within this {@link ParameterizedModel}
   */
  private Stream<String> getAllExpressions(ParameterizedModel model, ComponentAst componentAst) {
    return model.getParameterGroupModels().stream()
        .flatMap(groupModel -> groupModel.getParameterModels().stream()
            .filter(parameter -> !parameter.getExpressionSupport().equals(ExpressionSupport.NOT_SUPPORTED))
            .map(parameter -> componentAst.getParameter(groupModel.getName(), parameter.getName()).getRawValue())
            .filter(Objects::nonNull));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    if (component.recursiveStream().anyMatch(componentAst -> componentAst.getModel(ParameterizedModel.class)
        .map(model -> getAllExpressions(model, componentAst).anyMatch(this::isInsecure)).orElse(false))) {
      return of(create(component, this, "Using an insecure function within an operation. That's malo, feo, caca"));
    } else {
      return empty();
    }
  }
}
