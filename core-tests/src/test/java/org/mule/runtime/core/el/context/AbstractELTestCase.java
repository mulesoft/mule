/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.mule.mvel2.ImmutableElementException;
import org.mule.mvel2.PropertyAccessException;
import org.mule.mvel2.optimizers.OptimizerFactory;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.util.ExceptionUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public abstract class AbstractELTestCase extends AbstractMuleContextTestCase {

  protected Variant variant;
  protected ExpressionLanguage expressionLanguage;
  protected Flow flowConstruct;

  public AbstractELTestCase(Variant variant, String mvelOptimizer) {
    this.variant = variant;
    OptimizerFactory.setDefaultOptimizer(mvelOptimizer);
  }

  @Before
  public void setupFlowConstruct() throws Exception {
    flowConstruct = getTestFlow();
  }

  @Before
  public void setupExpressionEvaluator() throws Exception {
    expressionLanguage = getExpressionLanguage();
    if (expressionLanguage instanceof Initialisable) {
      ((Initialisable) expressionLanguage).initialise();
    }
  }

  @SuppressWarnings("deprecation")
  protected Object evaluate(String expression) {
    switch (variant) {
      case EVALUATOR_LANGUAGE:
        return expressionLanguage.evaluate(expression);
      case EXPRESSION_MANAGER:
        return muleContext.getExpressionManager().evaluate(expression, null, flowConstruct);
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  protected Object evaluate(String expression, MuleEvent event) {
    switch (variant) {
      case EVALUATOR_LANGUAGE:
        return expressionLanguage.evaluate(expression, event, flowConstruct);
      case EXPRESSION_MANAGER:
        return muleContext.getExpressionManager().evaluate(expression, event, flowConstruct);
    }
    return null;
  }

  public static enum Variant {
    EXPRESSION_MANAGER, EVALUATOR_LANGUAGE
  }

  @Parameters
  public static List<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{Variant.EVALUATOR_LANGUAGE, OptimizerFactory.DYNAMIC},
        {Variant.EVALUATOR_LANGUAGE, OptimizerFactory.SAFE_REFLECTIVE}, {Variant.EXPRESSION_MANAGER, OptimizerFactory.DYNAMIC},
        {Variant.EXPRESSION_MANAGER, OptimizerFactory.SAFE_REFLECTIVE}});
  }

  protected ExpressionLanguage getExpressionLanguage() throws Exception {
    return new MVELExpressionLanguage(muleContext);
  }

  protected void assertUnsupportedOperation(String expression) {
    try {
      evaluate(expression);
      fail("ExpressionRuntimeException expected");
    } catch (ExpressionRuntimeException e) {
      assertEquals(UnsupportedOperationException.class, ExceptionUtils.getRootCause(e).getClass());
    }
  }

  protected void assertUnsupportedOperation(String expression, MuleEvent event) {
    try {
      evaluate(expression, event);
      fail("ExpressionRuntimeException expected");
    } catch (ExpressionRuntimeException e) {
      assertEquals(UnsupportedOperationException.class, ExceptionUtils.getRootCause(e).getClass());
    }
  }

  protected void assertImmutableVariable(String expression) {
    try {
      evaluate(expression);
      fail("ExpressionRuntimeException expected");
    } catch (ExpressionRuntimeException e) {
      assertEquals(ImmutableElementException.class, ExceptionUtils.getRootCause(e).getClass());
    }
  }

  protected void assertImmutableVariable(String expression, MuleEvent event) {
    try {
      evaluate(expression, event);
      fail("ExpressionRuntimeException expected");
    } catch (ExpressionRuntimeException e) {
      assertEquals(ImmutableElementException.class, ExceptionUtils.getRootCause(e).getClass());
    }
  }

  protected void assertFinalProperty(String expression) {
    try {
      evaluate(expression);
      fail("ExpressionRuntimeException expected");
    } catch (ExpressionRuntimeException e) {
      assertEquals(PropertyAccessException.class, ExceptionUtils.getRootCause(e).getClass());
    }
  }

  protected void assertFinalProperty(String expression, MuleEvent event) {
    try {
      evaluate(expression, event);
      fail("ExpressionRuntimeException expected");
    } catch (ExpressionRuntimeException e) {
      assertEquals(PropertyAccessException.class, ExceptionUtils.getRootCause(e).getClass());
    }
  }

}
