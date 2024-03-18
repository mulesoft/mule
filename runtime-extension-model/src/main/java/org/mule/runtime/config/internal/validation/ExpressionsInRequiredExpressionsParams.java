/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENFORCE_REQUIRED_EXPRESSION_VALIDATION;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.MuleAstUtils.hasPropertyPlaceholder;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.core.internal.expression.util.ExpressionUtils.isExpression;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getGroupAndParametersPairs;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.core.internal.extension.AllowsExpressionWithoutMarkersModelProperty;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Expressions are provided for parameters that require expressions.
 */
public class ExpressionsInRequiredExpressionsParams implements Validation {

  private static final String CONFIGURATION_NAME = "configuration";

  protected static final ComponentIdentifier CONFIGURATION_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(CONFIGURATION_NAME).build();

  private final Optional<FeatureFlaggingService> featureFlaggingService;
  private final boolean ignoreParamsWithProperties;

  public ExpressionsInRequiredExpressionsParams(Optional<FeatureFlaggingService> featureFlaggingService,
                                                boolean ignoreParamsWithProperties) {
    this.featureFlaggingService = featureFlaggingService;
    this.ignoreParamsWithProperties = ignoreParamsWithProperties;
  }

  @Override
  public String getName() {
    return "Expression must be set in expressionsRequired params";
  }

  @Override
  public String getDescription() {
    return "Expressions are provided for parameters that require expressions.";
  }

  @Override
  public Level getLevel() {
    // According to the extension model, no collections or target-value for foreach, parallel-foreach, etc...
    // must be defined by an expression, but this was not enforced. This check is needed to avoid breaking on
    // legacy cases
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getModel(ParameterizedModel.class)
        .map(pmz -> pmz.getAllParameterModels())
        .orElse(emptyList())
        .stream()
        .filter(pm -> REQUIRED.equals(pm.getExpressionSupport())
            && !pm.getModelProperty(AllowsExpressionWithoutMarkersModelProperty.class).isPresent())
        .findAny()
        .isPresent());
  }

  @Override
  public List<ValidationResultItem> validateMany(ComponentAst component, ArtifactAst artifact) {
    return component.getModel(ParameterizedModel.class)
        .map(pmz -> getGroupAndParametersPairs(pmz)
            .filter(gnp -> REQUIRED.equals(gnp.getSecond().getExpressionSupport())
                && !gnp.getSecond().getModelProperty(AllowsExpressionWithoutMarkersModelProperty.class).isPresent())
            .map(gnp -> component.getParameter(gnp.getFirst().getName(), gnp.getSecond().getName()))
            .collect(toList()))
        .orElse(emptyList())
        .stream()
        .filter(param -> !hasPropertyPlaceholder(param.getRawValue()))
        .filter(param -> {
          if (param.getValueOrResolutionError().isRight() && param.getResolvedRawValue() instanceof String) {
            final String stringValue = param.getResolvedRawValue();
            if (!isExpression(stringValue)) {
              if (param.getModel().getName().equals("targetValue")) {
                return featureFlaggingService.map(ffs -> ffs.isEnabled(ENFORCE_REQUIRED_EXPRESSION_VALIDATION)).orElse(true);
              }
              return true;
            }
          }

          return false;
        })
        .map(param -> create(component, param, this,
                             format("A static value ('%s') was given for parameter '%s' but it requires an expression",
                                    param.getRawValue(), param.getModel().getName())))
        .collect(toList());
  }
}
