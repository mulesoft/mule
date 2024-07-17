/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.extension;

import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.ANY_TYPE;

import static java.lang.String.format;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.privileged.metadata.InternalMetadataContext;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link InputTypeResolver} implementation used for the "collection" parameter of the ForEach and ParallelForEach scopes.
 *
 * @since 4.8.0
 */
public class ForEachCollectionTypeResolver implements InputTypeResolver<String> {

  @Override
  public String getCategoryName() {
    return "FOREACH";
  }

  @Override
  public String getResolverName() {
    return "FOREACH_INPUT_COLLECTION";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    if (context instanceof InternalMetadataContext) {
      InternalMetadataContext internalContext = (InternalMetadataContext) context;
      if (internalContext.getExpressionLanguageMetadataService().isPresent() && internalContext.getTypeBindings().isPresent()) {
        String expression = sanitizeExpression(key);
        MetadataType resolvedType = resolveExpressionType(internalContext.getTypeBindings().get(),
                                                          internalContext.getExpressionLanguageMetadataService().get(),
                                                          expression);
        if (!(resolvedType instanceof ArrayType)) {
          throw new IllegalArgumentException(format("Expression `%s` does not resolve to a collection", expression));
        }
        return ((ArrayType) resolvedType).getType();
      }
    }

    // Fallback to any
    return ANY_TYPE;
  }

  private String sanitizeExpression(String expression) {
    String sanitizedExpression;
    if (expression.startsWith("#[") && expression.endsWith("]")) {
      sanitizedExpression = expression.substring("#[".length(), expression.length() - "]".length());
    } else {
      sanitizedExpression = expression;
    }

    return sanitizedExpression;
  }

  private MetadataType resolveExpressionType(TypeBindings typeBindings,
                                             ExpressionLanguageMetadataService expressionLanguageMetadataService,
                                             String expression)
      throws MetadataResolvingException {
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

  private static class ErrorTrackingMetadataServiceCallback implements ExpressionLanguageMetadataService.MessageCallback {

    private final List<String> errorsFound = new ArrayList<>();

    @Override
    public void warning(String message, ExpressionLanguageMetadataService.MessageLocation location) {
      // Does nothing
    }

    @Override
    public void error(String message, ExpressionLanguageMetadataService.MessageLocation location) {
      errorsFound.add(message);
    }

    public List<String> getErrorsFound() {
      return errorsFound;
    }
  }
}
