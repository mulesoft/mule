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
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.Event;
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

  protected ExpressionLanguage expressionLanguage;
  protected Flow flowConstruct;
  protected EventContext context;

  public AbstractELTestCase(String mvelOptimizer) {
    OptimizerFactory.setDefaultOptimizer(mvelOptimizer);
  }

  @Before
  public void setupFlowConstruct() throws Exception {
    flowConstruct = getTestFlow();
  }

  @Before
  public void setupMessageContext() throws Exception {
    context = DefaultMessageContext.create(flowConstruct, TEST_CONNECTOR);
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
    return expressionLanguage.evaluate(expression);
  }

  @SuppressWarnings("deprecation")
  protected Object evaluate(String expression, Event event) {
    return expressionLanguage.evaluate(expression, event, flowConstruct);
  }

  @SuppressWarnings("deprecation")
  protected Object evaluate(String expression, Event event, Event.Builder eventBuilder) {
    return expressionLanguage.evaluate(expression, event, eventBuilder, flowConstruct);
  }

  public static enum Variant {
    EXPRESSION_MANAGER, EVALUATOR_LANGUAGE
  }

  @Parameters
  public static List<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{OptimizerFactory.DYNAMIC}, {OptimizerFactory.SAFE_REFLECTIVE}});
  }

  protected ExpressionLanguage getExpressionLanguage() {
    final MVELExpressionLanguage el = new MVELExpressionLanguage(muleContext);
    return el;
  }

  protected void assertUnsupportedOperation(String expression) {
    try {
      evaluate(expression);
      fail("ExpressionRuntimeException expected");
    } catch (ExpressionRuntimeException e) {
      assertEquals(UnsupportedOperationException.class, ExceptionUtils.getRootCause(e).getClass());
    }
  }

  protected void assertUnsupportedOperation(String expression, Event event) {
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

  protected void assertImmutableVariable(String expression, Event event) {
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

  protected void assertFinalProperty(String expression, Event event) {
    try {
      evaluate(expression, event);
      fail("ExpressionRuntimeException expected");
    } catch (ExpressionRuntimeException e) {
      assertEquals(PropertyAccessException.class, ExceptionUtils.getRootCause(e).getClass());
    }
  }

}
