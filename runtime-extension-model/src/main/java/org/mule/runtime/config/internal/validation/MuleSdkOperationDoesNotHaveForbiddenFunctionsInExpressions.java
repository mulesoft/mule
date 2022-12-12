/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.el.validation.ScopePhaseValidationItemKind.DEPRECATED;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.config.internal.validation.ValidationUtils.locationToAdditionalData;
import static org.mule.runtime.core.internal.util.ExpressionUtils.getUnfixedExpression;
import static org.mule.runtime.core.internal.util.ExpressionUtils.isExpression;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.validation.ScopePhaseValidationItem;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MuleSdkOperationDoesNotHaveForbiddenFunctionsInExpressions extends MuleSdkOperationValidation {

  private final ExpressionLanguage expressionLanguage;

  private static final String SINCE_PARAMETER = "since";
  private static final String FUNCTION_PARAMETER = "function";
  private static final MuleVersion DEPRECATE_UP_TO = new MuleVersion("2.5.0");
  private static final Set<String> DEPRECATION_DOMAINS = ImmutableSet.of("dw", "Mule");

  public MuleSdkOperationDoesNotHaveForbiddenFunctionsInExpressions(ExpressionLanguage el) {
    this.expressionLanguage = el;
  }

  @Override
  public String getName() {
    return "Mule SDK Operation doesn't have deprecated expressions";
  }

  @Override
  public String getDescription() {
    return "The Mule SDK operation does not have a function as deprecated prior to DW 2.5 within itself";
  }

  private boolean isDeprecableProduct(ScopePhaseValidationItem warning) {
    String product = warning.getParams().get(FUNCTION_PARAMETER).split("\\::")[0];
    return DEPRECATION_DOMAINS.contains(product);
  }

  private boolean isDeprecatedVersion(ScopePhaseValidationItem warning) {
    MuleVersion since = new MuleVersion(warning.getParams().get(SINCE_PARAMETER));
    return since.sameAs(DEPRECATE_UP_TO) || since.priorTo(DEPRECATE_UP_TO);
  }

  private boolean isInvalidExpression(ScopePhaseValidationItem warning) {
    return warning.getKind().equals(DEPRECATED) && isDeprecatedVersion(warning) && isDeprecableProduct(warning);
  }

  private List<ScopePhaseValidationItem> getWarningMessages(String expression) {
    String actualExpression = getUnfixedExpression(expression);
    return expressionLanguage.collectScopePhaseValidationMessages(actualExpression, "", TypeBindings.builder().build())
        .getWarnings().stream().filter(this::isInvalidExpression).collect(toList());
  }

  /**
   * @return All the (raw) expressions within this {@link ParameterizedModel}
   */
  private List<ComponentParameterAst> getAllExpressions(ParameterizedModel model, ComponentAst componentAst) {
    return model.getParameterGroupModels().stream()
        .flatMap(groupModel -> groupModel.getParameterModels().stream()
            .filter(parameter -> !parameter.getExpressionSupport().equals(NOT_SUPPORTED))
            .map(parameter -> componentAst.getParameter(groupModel.getName(), parameter.getName())))
        .filter(param -> param.getRawValue() != null && isExpression(param.getRawValue()))
        .collect(toList());
  }

  private List<Pair<ComponentParameterAst, ScopePhaseValidationItem>> getExpressionsWithWarnings(ComponentAst component) {
    List<ComponentParameterAst> expressions = new ArrayList<>();
    component.recursiveStream().forEach(componentAst -> componentAst.getModel(ParameterizedModel.class)
        .map(model -> getAllExpressions(model, componentAst)).ifPresent(expressions::addAll));

    List<Pair<ComponentParameterAst, ScopePhaseValidationItem>> warnings = new ArrayList<>();
    for (ComponentParameterAst param : expressions) {
      for (ScopePhaseValidationItem item : getWarningMessages(param.getRawValue())) {
        warnings.add(new Pair<>(param, item));
      }
    }
    return warnings;
  }

  @Override
  public List<ValidationResultItem> validateMany(ComponentAst component, ArtifactAst artifact) {
    return getExpressionsWithWarnings(component).stream()
        .map(pair -> create(component, pair.getFirst(), this,
                            "Using an invalid function within a Mule SDK operation. All functions deprecated up to DataWeave 2.5 cannot be used inside a Mule Operation. Expression: "
                                + pair.getFirst().getRawValue(),
                            locationToAdditionalData(pair.getSecond().getLocation())))
        .collect(Collectors.toList());
  }
}
