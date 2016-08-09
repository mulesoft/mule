/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.el.mvel.datatype;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.mvel2.MVEL.compileExpression;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.api.metadata.MediaType.UNKNOWN;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.mvel2.MVEL;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.mvel2.integration.impl.CachedMapVariableResolverFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.el.mvel.DelegateVariableResolverFactory;
import org.mule.runtime.core.el.mvel.GlobalVariableResolverFactory;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguageContext;
import org.mule.runtime.core.el.mvel.MessageVariableResolverFactory;
import org.mule.runtime.core.el.mvel.StaticVariableResolverFactory;
import org.mule.runtime.core.el.mvel.VariableVariableResolverFactory;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractVarAssignmentDataTypePropagatorTestCase extends AbstractMuleContextTestCase {

  public static final Charset CUSTOM_ENCODING = StandardCharsets.UTF_16;
  public static final String PROPERTY_NAME = "foo";
  public static final String INNER_PROPERTY_NAME = "bar";

  private final EnricherDataTypePropagator dataTypePropagator;

  protected AbstractVarAssignmentDataTypePropagatorTestCase(EnricherDataTypePropagator dataTypePropagator) {
    this.dataTypePropagator = dataTypePropagator;
  }

  protected void doAssignmentDataTypePropagationTest(String expression) throws Exception {
    DataType expectedDataType = DataType.builder().type(String.class).mediaType(JSON).charset(CUSTOM_ENCODING).build();

    MuleEvent testEvent = getTestEvent(TEST_MESSAGE);

    CompiledExpression compiledExpression = compileMelExpression(expression, testEvent);

    dataTypePropagator.propagate(testEvent, new TypedValue(TEST_MESSAGE, expectedDataType), compiledExpression);

    assertThat(getVariableDataType(testEvent), like(String.class, JSON, CUSTOM_ENCODING));
  }

  protected void doInnerAssignmentDataTypePropagationTest(String expression) throws Exception {
    final DataType expectedDataType = DataType.builder().type(Map.class).mediaType(UNKNOWN).charset(CUSTOM_ENCODING).build();

    MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
    final Map<String, String> propertyValue = new HashMap<>();
    propertyValue.put(INNER_PROPERTY_NAME, TEST_MESSAGE);
    setVariable(testEvent, propertyValue, expectedDataType);

    CompiledExpression compiledExpression = compileMelExpression(expression, testEvent);

    // Attempts to propagate a different dataType, which should be ignored
    dataTypePropagator.propagate(testEvent, new TypedValue(propertyValue, DataType.STRING), compiledExpression);

    assertThat(getVariableDataType(testEvent), like(Map.class, UNKNOWN, CUSTOM_ENCODING));
  }

  protected abstract DataType getVariableDataType(MuleEvent event);

  protected abstract void setVariable(MuleEvent testEvent, Object propertyValue, DataType expectedDataType);

  private CompiledExpression compileMelExpression(String expression, MuleEvent testEvent) {
    final ParserConfiguration parserConfiguration = MVELExpressionLanguage.createParserConfiguration(Collections.EMPTY_MAP);
    final MVELExpressionLanguageContext context = createMvelExpressionLanguageContext(testEvent, parserConfiguration);

    CompiledExpression compiledExpression =
        (CompiledExpression) compileExpression(expression, new ParserContext(parserConfiguration));

    // Expression must be executed, otherwise the variable accessor is not properly configured
    MVEL.executeExpression(compiledExpression, context);

    return compiledExpression;
  }

  protected MVELExpressionLanguageContext createMvelExpressionLanguageContext(MuleEvent testEvent,
                                                                              ParserConfiguration parserConfiguration) {
    final MVELExpressionLanguageContext context = new MVELExpressionLanguageContext(parserConfiguration, muleContext);
    final StaticVariableResolverFactory staticContext = new StaticVariableResolverFactory(parserConfiguration, muleContext);
    final GlobalVariableResolverFactory globalContext =
        new GlobalVariableResolverFactory(Collections.EMPTY_MAP, Collections.EMPTY_MAP, parserConfiguration, muleContext);

    final DelegateVariableResolverFactory innerDelegate =
        new DelegateVariableResolverFactory(globalContext,
                                            new VariableVariableResolverFactory(parserConfiguration, muleContext, testEvent));
    final DelegateVariableResolverFactory delegate =
        new DelegateVariableResolverFactory(staticContext, new MessageVariableResolverFactory(parserConfiguration, muleContext,
                                                                                              testEvent, innerDelegate));
    context.setNextFactory(new CachedMapVariableResolverFactory(Collections.EMPTY_MAP, delegate));
    return context;
  }
}
