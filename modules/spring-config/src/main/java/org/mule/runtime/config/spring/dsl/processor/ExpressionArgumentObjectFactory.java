/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.expression.ExpressionConfig;
import org.mule.runtime.core.expression.transformers.ExpressionArgument;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import javax.inject.Inject;

/**
 * {@link ObjectFactory} that constructs {@link ExpressionArgument} from the mule configuration.
 *
 * @since 4.0
 */
public class ExpressionArgumentObjectFactory extends AbstractAnnotatedObjectFactory<ExpressionArgument> {

  @Inject
  private MuleContext muleContext;

  private String expression;
  private boolean optional;

  /**
   * @param expression the expression to retrieve the argument value.
   */
  public void setExpression(String expression) {
    this.expression = expression;
  }

  /**
   * @param optional true if the expression may resolve to null and do not fail, false otherwise.
   */
  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  @Override
  public ExpressionArgument doGetObject() throws Exception {
    ExpressionArgument expressionArgument = new ExpressionArgument(null, new ExpressionConfig(expression), optional);
    expressionArgument.setMuleContext(muleContext);
    return expressionArgument;
  }
}
