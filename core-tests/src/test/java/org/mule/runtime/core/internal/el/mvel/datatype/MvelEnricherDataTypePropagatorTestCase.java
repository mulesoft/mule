/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.mvel2.MVEL.compileExpression;

import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.InternalEvent.Builder;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.el.mvel.datatype.EnricherDataTypePropagator;
import org.mule.runtime.core.internal.el.mvel.datatype.MvelEnricherDataTypePropagator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MvelEnricherDataTypePropagatorTestCase extends AbstractMuleContextTestCase {

  public static final String MEL_EXPRESSION = "foo = bar";

  private final TypedValue typedValue = new TypedValue<>(TEST_MESSAGE, DataType.STRING);
  private final EnricherDataTypePropagator propagator1 = mock(EnricherDataTypePropagator.class);
  private final EnricherDataTypePropagator propagator2 = mock(EnricherDataTypePropagator.class);

  @Test
  public void invokesDataTypeAllPropagators() throws Exception {
    CompiledExpression compiledExpression = compileMelExpression();

    final List<EnricherDataTypePropagator> propagators = new ArrayList<>();
    propagators.add(propagator1);
    propagators.add(propagator2);

    MvelEnricherDataTypePropagator dataTypePropagator = new MvelEnricherDataTypePropagator(propagators);

    final Builder builder = InternalEvent.builder(testEvent());
    dataTypePropagator.propagate(typedValue, testEvent(), builder, compiledExpression);

    verify(propagator1).propagate(testEvent(), builder, typedValue, compiledExpression);
    verify(propagator2).propagate(testEvent(), builder, typedValue, compiledExpression);
  }

  @Test
  public void stopsCheckingDataTypePropagatorsAfterSuccessfulPropagation() throws Exception {
    CompiledExpression compiledExpression = compileMelExpression();

    final List<EnricherDataTypePropagator> propagators = new ArrayList<>();
    propagators.add(propagator1);
    final Builder builder = InternalEvent.builder(testEvent());
    when(propagator1.propagate(testEvent(), builder, typedValue, compiledExpression)).thenReturn(true);
    propagators.add(propagator2);

    MvelEnricherDataTypePropagator dataTypePropagator = new MvelEnricherDataTypePropagator(propagators);

    dataTypePropagator.propagate(typedValue, testEvent(), builder, compiledExpression);

    verify(propagator1).propagate(testEvent(), builder, typedValue, compiledExpression);
    verify(propagator2, never()).propagate(testEvent(), builder, typedValue, compiledExpression);
  }

  private CompiledExpression compileMelExpression() {
    MVELExpressionLanguage expressionLanguage = new MVELExpressionLanguage(muleContext);
    return (CompiledExpression) compileExpression(MEL_EXPRESSION, new ParserContext(expressionLanguage.getParserConfiguration()));
  }

}
