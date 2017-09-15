/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

/**
 * {@link ValueResolver} interface for expression based {@link ValueResolver}, in addition to be able to resolve
 * a value, implementations of this {@link ValueResolver} also can communicate the used expression.
 *
 * @since 4.0
 */
public interface ExpressionBasedValueResolver<T> extends ValueResolver<T> {

  /**
   * @return The configured expression in the {@link ValueResolver}
   */
  String getExpression();
}
