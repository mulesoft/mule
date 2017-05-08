/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.runtime.core.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.expression.ExpressionConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;


public class ExpressionSplitterIteratorTestCase extends AbstractMuleTestCase {

  private Event muleEvent = mock(Event.class);
  private MuleContext muleContext = mock(MuleContext.class);
  private ExtendedExpressionManager expressionManager = mock(ExtendedExpressionManager.class);
  private final List<TypedValue<?>> integers = createListOfIntegers();
  private final ExpressionConfig expressionConfig = mock(ExpressionConfig.class);
  private final ExpressionSplitter expressionSplitter = new ExpressionSplitter(expressionConfig);
  private final FlowCallStack flowCallStack = mock(FlowCallStack.class);

  @Before
  public void setUp() throws Exception {
    expressionSplitter.setMuleContext(muleContext);
    when(muleContext.getExpressionManager()).thenReturn(expressionManager);
    when(expressionConfig.getFullExpression()).thenReturn("fullExpression");
    when(expressionManager.split(any(String.class), any(Integer.class), any(Event.class), any(BindingContext.class)))
        .thenReturn(integers.iterator());
    when(muleEvent.getFlowCallStack()).thenReturn(flowCallStack);
    when(muleEvent.getError()).thenReturn(Optional.empty());
    expressionSplitter.initialise();
  }

  @Test
  public void testExpressionSplitterWithIteratorInput() throws Exception {
    List<?> values = expressionSplitter.splitMessage(muleEvent);
    assertThat(values.size(), is(integers.size()));
    assertListValues(values);
  }

  private List<TypedValue<?>> createListOfIntegers() {
    List<TypedValue<?>> integers = new ArrayList<>(3);
    for (int i = 0; i < 3; i++) {
      integers.add(new TypedValue<>(i, DataType.fromType(Integer.class)));
    }
    return integers;
  }

  private void assertListValues(List<?> values) {
    final Integer[] i = {0};
    values.forEach(value -> {
      TypedValue<?> typedValue = (TypedValue<?>) value;
      assertThat(typedValue.getValue(), instanceOf(Integer.class));
      assertThat(typedValue.getValue(), is(i[0]++));
    });
  }

}
