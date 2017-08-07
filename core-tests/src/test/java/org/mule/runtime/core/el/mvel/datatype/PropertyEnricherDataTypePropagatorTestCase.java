/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.el.mvel.datatype;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.mvel2.MVEL.compileExpression;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class PropertyEnricherDataTypePropagatorTestCase extends AbstractMuleContextTestCase {

  private static final Charset CUSTOM_ENCODING = StandardCharsets.UTF_16;

  private final EnricherDataTypePropagator dataTypePropagator = new PropertyEnricherDataTypePropagator();

  @Test
  public void propagatesDataTypeForInlinedInvocationProperty() throws Exception {
    final DataType expectedDataType = DataType.builder().type(String.class).mediaType(JSON).charset(CUSTOM_ENCODING).build();

    MVELExpressionLanguage expressionLanguage = new MVELExpressionLanguage(muleContext);
    final CompiledExpression compiledExpression =
        (CompiledExpression) compileExpression("foo = 'unused'", new ParserContext(expressionLanguage.getParserConfiguration()));

    Event testEvent = eventBuilder().message(of(TEST_MESSAGE)).addVariable("foo", "bar").build();

    final Builder builder = Event.builder(testEvent);
    dataTypePropagator.propagate(testEvent, builder, new TypedValue<>(TEST_MESSAGE, expectedDataType), compiledExpression);

    assertThat(builder.build().getVariables().get("foo").getDataType(), like(String.class, JSON, CUSTOM_ENCODING));
  }

  @Test
  public void propagatesDataTypeForInlinedSessionProperty() throws Exception {
    final DataType expectedDataType = DataType.builder().type(String.class).mediaType(JSON).charset(CUSTOM_ENCODING).build();

    MVELExpressionLanguage expressionLanguage = new MVELExpressionLanguage(muleContext);
    final CompiledExpression compiledExpression =
        (CompiledExpression) compileExpression("foo = 'unused'", new ParserContext(expressionLanguage.getParserConfiguration()));

    testEvent().getSession().setProperty("foo", "bar");

    final Builder builder = Event.builder(testEvent());
    dataTypePropagator.propagate(testEvent(), builder, new TypedValue(TEST_MESSAGE, expectedDataType), compiledExpression);

    assertThat(builder.build().getSession().getPropertyDataType("foo"), like(String.class, JSON, CUSTOM_ENCODING));
  }

}
