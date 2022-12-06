/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.Integer.parseInt;
import static org.mule.runtime.api.el.validation.ScopePhaseValidationItemKind.DEPRECATED;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.internal.util.ExpressionUtils.getUnfixedExpression;
import static org.mule.runtime.core.internal.util.ExpressionUtils.isExpression;

import com.google.common.collect.ImmutableSet;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.validation.ScopePhaseValidationItem;
import org.mule.runtime.api.el.validation.ScopePhaseValidationMessages;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class OperationDoesNotHaveDeprecatedExpression extends OperationValidation {

  private final ExpressionLanguage expressionLanguage;

  private static final String SINCE_PARAMETER = "since";
  private static final String FUNCTION_PARAMETER = "function";
  private static final Version DEPRECATE_PREVIOUS_TO = new Version("2.5.0");
  private static final Set<String> DEPRECATION_DOMAINS = ImmutableSet.of("dw", "Mule");

  public OperationDoesNotHaveDeprecatedExpression(ExpressionLanguage el) {
    this.expressionLanguage = el;
  }

  @Override
  public String getName() {
    return "Operation doesn't have deprecated expressions";
  }

  @Override
  public String getDescription() {
    return "The operation does not have a function as deprecated prior to DW 2.5 within itself";
  }

  private boolean isDeprecableProduct(ScopePhaseValidationItem warning) {
    String product = warning.getParams().get(FUNCTION_PARAMETER).split("\\::")[0];
    return DEPRECATION_DOMAINS.contains(product);
  }

  private boolean isDeprecatedVersion(ScopePhaseValidationItem warning) {
    return new Version(warning.getParams().get(SINCE_PARAMETER)).isPriorOrEqualTo(DEPRECATE_PREVIOUS_TO);
  }

  private boolean isInvalidExpression(String expression) {
    if (!isExpression(expression)) {
      return false;
    }
    String actualExpression = getUnfixedExpression(expression);
    ScopePhaseValidationMessages result =
        expressionLanguage.collectScopePhaseValidationMessages(actualExpression, "", TypeBindings.builder().build());

    return result.getWarnings().stream().anyMatch(warning -> warning.getKind().equals(DEPRECATED) && isDeprecatedVersion(warning)
        && isDeprecableProduct(warning)) || !result.getErrors().isEmpty();
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
        .map(model -> getAllExpressions(model, componentAst).anyMatch(this::isInvalidExpression)).orElse(false))) {
      return of(create(component, this,
                       "Using an invalid function within an operation. All functions deprecated up to DataWeave 2.5 cannot be used inside a Mule Operation"));
    } else {
      return empty();
    }
  }

  private static class Version {

    private int mayor, minor, patch;

    public Version(String v) {
      String[] parts = v.split("\\.");
      mayor = parseInt(parts[0]);
      minor = parseInt(parts[1]);
      if (parts.length > 2) {
        patch = parseInt(parts[2]);
      } else {
        patch = 0;
      }
    }

    public boolean isPriorTo(Version other) {
      if (other.mayor != this.mayor) {
        return other.mayor > this.mayor;
      }
      if (other.minor != this.minor) {
        return other.minor > this.minor;
      }
      return other.patch > this.patch;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Version)) {
        return false;
      }
      Version other = (Version) o;
      return other.mayor == this.mayor && other.minor == this.minor && other.patch == this.patch;
    }

    public boolean isPriorOrEqualTo(Version other) {
      return isPriorTo(other) || equals(other);
    }
  }
}
