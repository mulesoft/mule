/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.sanitize;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService.MessageCallback;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService.MessageLocation;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.api.tooling.metadata.ParameterExpressionMetadataResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An abstract implementation of {@link ParameterExpressionMetadataResolver}.
 * <p>
 * Takes care of resolving the expression output types and handling errors while abstracting from how the parameter values are
 * obtained.
 */
abstract class AbstractParameterExpressionMetadataResolver implements ParameterExpressionMetadataResolver {

  private final TypeBindings typeBindings;
  private final ExpressionManager expressionManager;
  private final ExpressionLanguageMetadataService expressionLanguageMetadataService;

  public AbstractParameterExpressionMetadataResolver(TypeBindings typeBindings, ExpressionManager expressionManager,
                                                     ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    this.typeBindings = typeBindings;
    this.expressionManager = expressionManager;
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;
  }

  /**
   * @return The model and actual value for the given parameter name.
   */
  protected abstract Optional<Pair<ParameterModel, Object>> getParameter(String parameterName);

  @Override
  public Optional<MetadataResult<MetadataType>> getActualInputMetadataIfExpression(String parameterName) {
    Optional<Pair<ParameterModel, Object>> parameter = getParameter(parameterName);
    return parameter.flatMap(p -> resolveParameter(p.getFirst(), p.getSecond()));
  }

  private Optional<MetadataResult<MetadataType>> resolveParameter(ParameterModel parameterModel, Object value) {
    if (!skip(parameterModel) && allowsExpressions(parameterModel) && isExpression(value)) {
      try {
        MetadataType actualParameterType = resolveExpressionType(sanitize((String) value));
        return of(success(actualParameterType));
      } catch (MetadataResolvingException e) {
        return of(failure(newFailure().onParameter(parameterModel.getName())));
      }
    }
    return empty();
  }

  private boolean skip(ParameterModel parameterModel) {
    // Skip these because their type can only be determined at the output
    return TARGET_VALUE_PARAMETER_NAME.equals(parameterModel.getName()) ||
        TARGET_PARAMETER_NAME.equals(parameterModel.getName());
  }

  private boolean allowsExpressions(ParameterModel parameterModel) {
    return !NOT_SUPPORTED.equals(parameterModel.getExpressionSupport());
  }

  private boolean isExpression(Object value) {
    return value instanceof String && expressionManager.isExpression((String) value);
  }

  private MetadataType resolveExpressionType(String expression) throws MetadataResolvingException {
    ErrorTrackingMetadataServiceCallback errorsTracker = new ErrorTrackingMetadataServiceCallback();
    MetadataType resolvedType =
        expressionLanguageMetadataService.getOutputType(typeBindings, expression, errorsTracker);
    if (errorsTracker.getErrorsFound().isEmpty()) {
      return resolvedType;
    } else {
      throw new MetadataResolvingException(format("Error resolving metadata from expression %s: %s", expression,
                                                  errorsTracker.getErrorsFound().get(0)),
                                           UNKNOWN);
    }
  }

  private static class ErrorTrackingMetadataServiceCallback implements MessageCallback {

    private final List<String> errorsFound = new ArrayList<>();

    @Override
    public void warning(String message, MessageLocation location) {
      // Does nothing
    }

    @Override
    public void error(String message, MessageLocation location) {
      errorsFound.add(message);
    }

    public List<String> getErrorsFound() {
      return errorsFound;
    }
  }
}
