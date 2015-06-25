/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.transformer.DataType.STRING_DATA_TYPE;
import static org.mule.mvel2.MVEL.compileExpression;
import org.mule.api.MuleMessage;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.TypedValue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MvelEnricherDataTypePropagatorTestCase extends AbstractMuleContextTestCase
{

    public static final String MEL_EXPRESSION = "foo = bar";

    private final MuleMessage message = mock(MuleMessage.class);
    private final TypedValue typedValue = new TypedValue(TEST_MESSAGE, STRING_DATA_TYPE);
    private final EnricherDataTypePropagator propagator1 = mock(EnricherDataTypePropagator.class);
    private final EnricherDataTypePropagator propagator2 = mock(EnricherDataTypePropagator.class);

    @Test
    public void invokesDataTypeAllPropagators() throws Exception
    {
        CompiledExpression compiledExpression = compileMelExpression();

        final List<EnricherDataTypePropagator> propagators = new ArrayList<>();
        propagators.add(propagator1);
        propagators.add(propagator2);

        MvelEnricherDataTypePropagator dataTypePropagator = new MvelEnricherDataTypePropagator(propagators);

        dataTypePropagator.propagate(typedValue, message, compiledExpression);

        verify(propagator1).propagate(message, typedValue, compiledExpression);
        verify(propagator2).propagate(message, typedValue, compiledExpression);
    }

    @Test
    public void stopsCheckingDataTypePropagatorsAfterSuccessfulPropagation() throws Exception
    {
        CompiledExpression compiledExpression = compileMelExpression();

        final List<EnricherDataTypePropagator> propagators = new ArrayList<>();
        propagators.add(propagator1);
        when(propagator1.propagate(message, typedValue, compiledExpression)).thenReturn(true);
        propagators.add(propagator2);

        MvelEnricherDataTypePropagator dataTypePropagator = new MvelEnricherDataTypePropagator(propagators);

        dataTypePropagator.propagate(typedValue, message, compiledExpression);

        verify(propagator1).propagate(message, typedValue, compiledExpression);
        verify(propagator2, never()).propagate(message, typedValue, compiledExpression);
    }

    private CompiledExpression compileMelExpression()
    {
        MVELExpressionLanguage expressionLanguage = (MVELExpressionLanguage) muleContext.getExpressionLanguage();
        return (CompiledExpression) compileExpression(MEL_EXPRESSION, new ParserContext(expressionLanguage.getParserConfiguration()));
    }

}