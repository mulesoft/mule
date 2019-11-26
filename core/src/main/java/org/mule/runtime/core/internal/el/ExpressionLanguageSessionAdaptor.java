/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

import java.util.Iterator;

/**
 * Adaptor of {@link org.mule.runtime.api.el.ExpressionLanguageSession} that adapt API to
 * throw ExpressionRuntimeException.
 *
 * @since 1.2
 */
public interface ExpressionLanguageSessionAdaptor extends AutoCloseable {

  /**
   * Evaluates an expression according to the parameters used during construction and the global bindings.
   *
   * @param expression the EL expression
   * @return the result of the expression plus its type
   * @throws ExpressionExecutionException when an error occurs during evaluation
   */
  TypedValue<?> evaluate(String expression) throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to the parameters used during construction, the global bindings and the {@link DataType} of
   * the expected result.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value
   *        type.
   * @return the result of the expression plus its type
   * @throws ExpressionExecutionException or during transformation or during transformation
   */
  TypedValue<?> evaluate(String expression, DataType expectedOutputType) throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to the parameters used during construction and the global bindings.
   *
   * @param expression the EL expression
   * @param timeout how long to wait for the expression to be evaluated, in milliseconds. If the evaluation takes more than this
   *        time, an {@link ExpressionExecutionException} will be thrown.
   * @return the result of the expression plus its type
   * @throws ExpressionExecutionException when an error occurs during evaluation
   */
  TypedValue<?> evaluate(String expression, long timeout) throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext} and the global one.
   * <p>
   * The evaluation of this script will do a best effort to avoid failing when the result value can not be represented in the
   * corresponding format.
   *
   * @param expression the EL expression
   * @return the result of the expression plus its type
   * @throws ExpressionExecutionException when an error occurs during evaluation
   */
  TypedValue<?> evaluateLogExpression(String expression) throws ExpressionRuntimeException;

  /**
   * Splits using the specified expression. The expression should return a collection of elements or an object. In case of the
   * object it will iterate through the entries
   *
   * @param expression the expression to be used to split
   * @return an iterator with the elements that were split
   */
  Iterator<TypedValue<?>> split(String expression);

  /**
   * Evaluates an expression according to the parameters used during construction and the global bindings.
   *
   * @param expression the EL expression
   * @return the result of the expression plus its type
   * @throws ExpressionExecutionException when an error occurs during evaluation
   *
   * @since 1.3
   */
  TypedValue<?> evaluate(CompiledExpression expression) throws ExpressionExecutionException;

  /**
   * Evaluates an expression according to the parameters used during construction, the global bindings and the {@link DataType} of
   * the expected result.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value
   *        type.
   * @return the result of the expression plus its type
   * @throws ExpressionExecutionException or during transformation or during transformation
   *
   * @since 1.3
   */
  TypedValue<?> evaluate(CompiledExpression expression, DataType expectedOutputType) throws ExpressionExecutionException;

  /**
   * Evaluates an expression according to the parameters used during construction and the global bindings.
   *
   * @param expression the EL expression
   * @param timeout how long to wait for the expression to be evaluated, in milliseconds. If the evaluation takes more than this
   *        time, an {@link ExpressionExecutionException} will be thrown.
   * @return the result of the expression plus its type
   * @throws ExpressionExecutionException when an error occurs during evaluation
   *
   * @since 1.3
   */
  TypedValue<?> evaluate(CompiledExpression expression, long timeout) throws ExpressionExecutionException;

  /**
   * Evaluates an expression according to a given {@link BindingContext} and the global one.
   * <p>
   * The evaluation of this script will do a best effort to avoid failing when the result value can not be represented in the
   * corresponding format.
   *
   * @param expression the EL expression
   * @return the result of the expression plus its type
   * @throws ExpressionExecutionException when an error occurs during evaluation
   *
   * @since 1.3
   */
  TypedValue<?> evaluateLogExpression(CompiledExpression expression) throws ExpressionExecutionException;

  /**
   * Splits using the specified expression. The expression should return a collection of elements or an object. In case of the
   * object it will iterate through the entries
   *
   * @param expression the expression to be used to split
   * @return an iterator with the elements that were split
   *
   * @since 1.3
   */
  Iterator<TypedValue<?>> split(CompiledExpression expression);

  /**
   * Frees any resources used to maintain this context.
   */
  @Override
  void close();
}
