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
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class PropertyExpressionDataTypeResolverTestCase extends AbstractMuleContextTestCase
{

    public static final String EXPRESSION_VALUE = "bar";
    public static final String CUSTOM_ENCODING = StandardCharsets.UTF_16.name();
    private final ExpressionDataTypeResolver expressionDataTypeResolver = new PropertyExpressionDataTypeResolver();

    @Test
    public void returnsInlineFlowVarDataType() throws Exception
    {
        final String expression = "foo";
        final DataType expectedDataType = DataTypeFactory.create(String.class, JSON);
        expectedDataType.setEncoding(CUSTOM_ENCODING);

        MVELExpressionLanguage expressionLanguage = (MVELExpressionLanguage) muleContext.getExpressionLanguage();
        final CompiledExpression compiledExpression = (CompiledExpression) compileExpression(expression, new ParserContext(expressionLanguage.getParserConfiguration()));

        MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
        testEvent.getMessage().setProperty("foo", EXPRESSION_VALUE, INVOCATION, expectedDataType);

        assertThat(expressionDataTypeResolver.resolve(testEvent.getMessage(), compiledExpression), like(String.class, JSON, CUSTOM_ENCODING));
    }

    @Test
    public void returnsInlineSessionPropertyDataType() throws Exception
    {
        final String expression = "foo";
        final DataType expectedDataType = DataTypeFactory.create(String.class, JSON);
        expectedDataType.setEncoding(CUSTOM_ENCODING);

        MVELExpressionLanguage expressionLanguage = (MVELExpressionLanguage) muleContext.getExpressionLanguage();
        final CompiledExpression compiledExpression = (CompiledExpression) compileExpression(expression, new ParserContext(expressionLanguage.getParserConfiguration()));

        MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
        testEvent.getMessage().setProperty("foo", EXPRESSION_VALUE, SESSION, expectedDataType);

        assertThat(expressionDataTypeResolver.resolve(testEvent.getMessage(), compiledExpression), like(String.class, JSON, CUSTOM_ENCODING));
    }

}