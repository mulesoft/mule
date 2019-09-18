/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.function;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.el.BindingContextUtils.ERROR;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.core.api.exception.DefaultErrorTypeMatcherFactory;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of causedBy() which determines whether an error matches a certain error type.
 *
 * @since 4.1
 */
public class CausedByFunction implements ExpressionFunction {

  private final ErrorTypeRepository errorTypeRepository;

  public CausedByFunction(ErrorTypeRepository errorTypeRepository) {
    this.errorTypeRepository = errorTypeRepository;
  }

  @Override
  public Object call(Object[] parameters, BindingContext context) {
    Error error = (Error) parameters[0];
    checkArgument(error != null, "There's no error to match against.");
    String errorIdentifier = (String) parameters[1];

    ErrorTypeMatcher errorTypeMatcher = new DefaultErrorTypeMatcherFactory().create(resolveErrorType(errorIdentifier));
    return errorTypeMatcher.match(error.getErrorType());
  }

  private ErrorType resolveErrorType(String errorIdentifier) {
    return errorTypeRepository.getErrorType(buildFromStringRepresentation(errorIdentifier))
        .orElseThrow(() -> new IllegalArgumentException(format("Could not find error type '%s'.", errorIdentifier)));
  }

  @Override
  public Optional<DataType> returnType() {
    return of(BOOLEAN);
  }

  @Override
  public List<FunctionParameter> parameters() {
    return Arrays.asList(new FunctionParameter("error", fromType(Error.class), context -> context.lookup(ERROR).orElse(null)),
                         new FunctionParameter("type", STRING));
  }
}
