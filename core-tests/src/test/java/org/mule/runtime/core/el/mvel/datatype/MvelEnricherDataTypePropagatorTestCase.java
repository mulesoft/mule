/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.el.mvel.datatype;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.mvel2.MVEL.compileExpression;

import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class MvelEnricherDataTypePropagatorTestCase extends AbstractMuleContextTestCase {

  public static final String MEL_EXPRESSION = "foo = bar";

  private Event event;
  private final DefaultTypedValue typedValue = new DefaultTypedValue(TEST_MESSAGE, DataType.STRING);
  private final EnricherDataTypePropagator propagator1 = mock(EnricherDataTypePropagator.class);
  private final EnricherDataTypePropagator propagator2 = mock(EnricherDataTypePropagator.class);

  @Before
  public void before() throws Exception {
    event = Event.builder(DefaultMessageContext.create(getTestFlow(), TEST_CONNECTOR)).build();
  }

  @Test
  public void invokesDataTypeAllPropagators() throws Exception {
    CompiledExpression compiledExpression = compileMelExpression();

    final List<EnricherDataTypePropagator> propagators = new ArrayList<>();
    propagators.add(propagator1);
    propagators.add(propagator2);

    MvelEnricherDataTypePropagator dataTypePropagator = new MvelEnricherDataTypePropagator(propagators);

    final Builder builder = Event.builder(event);
    dataTypePropagator.propagate(typedValue, event, builder, compiledExpression);

    verify(propagator1).propagate(event, builder, typedValue, compiledExpression);
    verify(propagator2).propagate(event, builder, typedValue, compiledExpression);
  }

  @Test
  public void stopsCheckingDataTypePropagatorsAfterSuccessfulPropagation() throws Exception {
    CompiledExpression compiledExpression = compileMelExpression();

    final List<EnricherDataTypePropagator> propagators = new ArrayList<>();
    propagators.add(propagator1);
    final Builder builder = Event.builder(event);
    when(propagator1.propagate(event, builder, typedValue, compiledExpression)).thenReturn(true);
    propagators.add(propagator2);

    MvelEnricherDataTypePropagator dataTypePropagator = new MvelEnricherDataTypePropagator(propagators);

    dataTypePropagator.propagate(typedValue, event, builder, compiledExpression);

    verify(propagator1).propagate(event, builder, typedValue, compiledExpression);
    verify(propagator2, never()).propagate(event, builder, typedValue, compiledExpression);
  }

  private CompiledExpression compileMelExpression() {
    MVELExpressionLanguage expressionLanguage = (MVELExpressionLanguage) muleContext.getExpressionLanguage();
    return (CompiledExpression) compileExpression(MEL_EXPRESSION, new ParserContext(expressionLanguage.getParserConfiguration()));
  }

}
