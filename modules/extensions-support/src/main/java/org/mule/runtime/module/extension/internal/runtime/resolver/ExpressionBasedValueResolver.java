/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

/**
 * {@link ValueResolver} interface for expression based {@link ValueResolver}, in addition to be able to resolve a value,
 * implementations of this {@link ValueResolver} also can communicate the used expression.
 *
 * @since 4.0
 */
public interface ExpressionBasedValueResolver<T> extends ValueResolver<T> {

  /**
   * @return The configured expression in the {@link ValueResolver}
   */
  String getExpression();
}
