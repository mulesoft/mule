/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.mvel2.MVEL.compileExpression;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.nio.charset.Charset;

public class PropertyExpressionDataTypeResolverTestCase extends AbstractMuleContextTestCase {

  public static final String EXPRESSION_VALUE = "bar";
  public static final Charset CUSTOM_ENCODING = UTF_16;
  private final ExpressionDataTypeResolver expressionDataTypeResolver = new PropertyExpressionDataTypeResolver();

  @Test
  public void returnsInlineFlowVarDataType() throws Exception {
    final String expression = "foo";
    final DataType expectedDataType = DataType.builder().type(String.class).mediaType(JSON).charset(CUSTOM_ENCODING).build();

    MVELExpressionLanguage expressionLanguage = new MVELExpressionLanguage(muleContext);
    final CompiledExpression compiledExpression =
        (CompiledExpression) compileExpression(expression, new ParserContext(expressionLanguage.getParserConfiguration()));

    PrivilegedEvent testEvent = this.<PrivilegedEvent.Builder>getEventBuilder()
        .message(of(TEST_MESSAGE))
        .addVariable("foo", EXPRESSION_VALUE, expectedDataType)
        .build();

    assertThat(expressionDataTypeResolver.resolve(testEvent, compiledExpression), like(String.class, JSON, CUSTOM_ENCODING));
  }

  @Test
  public void returnsInlineSessionPropertyDataType() throws Exception {
    final String expression = "foo";
    final DataType expectedDataType = DataType.builder().type(String.class).mediaType(JSON).charset(CUSTOM_ENCODING).build();

    MVELExpressionLanguage expressionLanguage = new MVELExpressionLanguage(muleContext);
    final CompiledExpression compiledExpression =
        (CompiledExpression) compileExpression(expression, new ParserContext(expressionLanguage.getParserConfiguration()));

    ((PrivilegedEvent) testEvent()).getSession().setProperty("foo", EXPRESSION_VALUE, expectedDataType);

    assertThat(expressionDataTypeResolver.resolve((PrivilegedEvent) testEvent(), compiledExpression),
               like(String.class, JSON, CUSTOM_ENCODING));
  }

}
