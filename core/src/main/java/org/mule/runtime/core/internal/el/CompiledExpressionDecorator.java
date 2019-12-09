/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import org.mule.runtime.api.el.CompiledExpression;

public interface CompiledExpressionDecorator extends CompiledExpression {

  static CompiledExpression unwrap(CompiledExpression compiledExpression) {
    return compiledExpression instanceof CompiledExpressionDecorator
        ? ((CompiledExpressionDecorator) compiledExpression).getDelegate()
        : compiledExpression;
  }

  CompiledExpression getDelegate();
}
