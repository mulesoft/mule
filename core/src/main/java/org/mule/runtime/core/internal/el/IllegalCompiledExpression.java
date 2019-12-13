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
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;

import java.util.List;
import java.util.Optional;

/**
 * A {@link CompiledExpression} implementation which represents a DataWeave expression that failed to be compiled.
 * <p>
 * The only reason for this class to exists is backwards compatibility. Before the concept of {@link CompiledExpression} was
 * introduced, invalid expressions would fail only when evaluated. Now that we are pre compiling all expressions, that failure
 * would occur at deployment time, meaning that apps with illegal expressions that were never executed will fail to deploy.
 * <p>
 * This class represents such an expression and holds the original {@link ExpressionCompilationException}. Because DataWeave does
 * not accept custom implementations of {@link CompiledExpression}, evaluation of this expression will fail. It is then
 * the {@link DataWeaveExpressionLanguageAdaptor} responsibility to propagate the {@link #getCompilationException()} exception
 * (it's a nasty job but somebody has to do it).
 *
 * @since 4.3.0
 */
public final class IllegalCompiledExpression implements CompiledExpression {

  private final String expression;
  private final ExpressionCompilationException compilationException;

  /**
   * Creates a new instance
   *
   * @param expression           the expressions that couldn't be compiled
   * @param compilationException the actual compilation exception
   */
  public IllegalCompiledExpression(String expression, ExpressionCompilationException compilationException) {
    this.expression = expression;
    this.compilationException = compilationException;
  }

  /**
   * @return the original compilation exception
   */
  public ExpressionCompilationException getCompilationException() {
    return compilationException;
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
}
