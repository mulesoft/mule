/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.api.transport.PropertyScope.INVOCATION;
import static org.mule.api.transport.PropertyScope.SESSION;
import static org.mule.mvel2.MVEL.compileExpression;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.transformer.types.MimeTypes.JSON;
import org.mule.api.MuleEvent;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class PropertyEnricherDataTypePropagatorTestCase extends AbstractMuleContextTestCase
{

    private static final String CUSTOM_ENCODING = StandardCharsets.UTF_16.name();

    private final EnricherDataTypePropagator dataTypePropagator = new PropertyEnricherDataTypePropagator();

    @Test
    public void propagatesDataTypeForInlinedInvocationProperty() throws Exception
    {
        doInlinePropertyDataTypePropagationTest(INVOCATION);
    }

    @Test
    public void propagatesDataTypeForInlinedSessionProperty() throws Exception
    {
        doInlinePropertyDataTypePropagationTest(SESSION);
    }

    private void doInlinePropertyDataTypePropagationTest(PropertyScope scope) throws Exception
    {
        final DataType expectedDataType = DataTypeFactory.create(String.class, JSON);
        expectedDataType.setEncoding(CUSTOM_ENCODING);

        MVELExpressionLanguage expressionLanguage = (MVELExpressionLanguage) muleContext.getExpressionLanguage();
        final CompiledExpression compiledExpression = (CompiledExpression) compileExpression("foo = 'unused'", new ParserContext(expressionLanguage.getParserConfiguration()));

        MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
        testEvent.getMessage().setProperty("foo", "bar", scope);


        dataTypePropagator.propagate(testEvent.getMessage(), new TypedValue(TEST_MESSAGE, expectedDataType), compiledExpression);

        assertThat(testEvent.getMessage().getPropertyDataType("foo", scope), like(String.class, JSON, CUSTOM_ENCODING));
    }
}