/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.spy;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.expression.DefaultExpressionManager;
import org.mule.metadata.api.model.MetadataType;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.function.Function;

import org.junit.Test;

public class ExpressionFunctionValueResolverTestCase extends AbstractMuleContextTestCase {

  private static final String INTEGER_EXPRESSION = "#[2+2]";
  private static final ExpressionFunction INTEGER_EXPRESSION_FUNCTION =
      new ExpressionFunction(INTEGER_EXPRESSION, toMetadataType(Integer.class));

  private MuleEvent event;
  private DefaultExpressionManager expressionManager;

  @Override
  protected void doSetUp() throws Exception {
    event = getTestEvent("test");
    expressionManager = spy((DefaultExpressionManager) muleContext.getExpressionManager());
    DefaultMuleContext defaultMuleContext = (DefaultMuleContext) muleContext;
    defaultMuleContext.setExpressionManager(expressionManager);
  }


  @Test
  public void testEqualExpressionFunctions() {
    assertThat(INTEGER_EXPRESSION_FUNCTION,
               is(equalTo(new ExpressionFunction(INTEGER_EXPRESSION, toMetadataType(Integer.class)))));
  }

  @Test
  public void testNotEqualExpressionFunctions() {
    ExpressionFunction anotherExpressionFunction = new ExpressionFunction("3", toMetadataType(Integer.class));
    assertThat(INTEGER_EXPRESSION_FUNCTION, is(not(equalTo(anotherExpressionFunction))));

    anotherExpressionFunction = new ExpressionFunction(INTEGER_EXPRESSION, toMetadataType(String.class));
    assertThat(INTEGER_EXPRESSION_FUNCTION, is(not(equalTo(anotherExpressionFunction))));
  }

  @Test
  public void testEvaluateExpressionFunction() throws Exception {
    Function<MuleEvent, Integer> function = getResolvedFunction(INTEGER_EXPRESSION, toMetadataType(Integer.class));
    assertExpressionFunction(function, 4);
  }

  @Test
  public void testEvaluateConstantValueExpressionFunction() throws Exception {
    Function<MuleEvent, Integer> function = getResolvedFunction("321", toMetadataType(Integer.class));
    assertExpressionFunction(function, 321);
  }

  private void assertExpressionFunction(Function<MuleEvent, Integer> function, Object value) {
    assertThat(function, is(not(nullValue())));
    Integer apply = function.apply(event);
    assertThat(apply, is(value));
  }

  public <T> Function<MuleEvent, T> getResolvedFunction(String expression, MetadataType type) throws MuleException {
    return new ExpressionFunctionValueResolver<T>(expression, type).resolve(event);
  }

}
