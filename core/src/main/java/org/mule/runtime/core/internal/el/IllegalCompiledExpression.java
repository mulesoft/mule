/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.util.Collections.emptyList;

import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.api.el.ModuleElementName;
import org.mule.runtime.api.metadata.MediaType;

import java.util.List;
import java.util.Optional;

public final class IllegalCompiledExpression implements CompiledExpression {

  private final String expression;
  private final ExpressionCompilationException compilationException;

  public IllegalCompiledExpression(String expression, ExpressionCompilationException compilationException) {
    this.expression = expression;
    this.compilationException = compilationException;
  }

  @Override
  public String expression() {
    return expression;
  }

  @Override
  public Optional<MediaType> outputType() {
    return Optional.empty();
  }

  @Override
  public List<ModuleElementName> externalDependencies() {
    return emptyList();
  }

  public ExpressionCompilationException getCompilationException() {
    return compilationException;
  }
}
