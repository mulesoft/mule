/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import static java.util.Collections.emptyMap;
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
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.internal.el.mvel.DelegateVariableResolverFactory;
import org.mule.runtime.core.internal.el.mvel.GlobalVariableResolverFactory;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguageContext;
import org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory;
import org.mule.runtime.core.internal.el.mvel.StaticVariableResolverFactory;
import org.mule.runtime.core.internal.el.mvel.VariableVariableResolverFactory;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
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

    final PrivilegedEvent.Builder builder = PrivilegedEvent.builder(testEvent());
    CompiledExpression compiledExpression = compileMelExpression(expression, (PrivilegedEvent) testEvent(), builder);
    PrivilegedEvent event = builder.build();
    dataTypePropagator.propagate(event, builder, new TypedValue<>(TEST_MESSAGE, expectedDataType), compiledExpression);
    event = builder.build();

    assertThat(getVariableDataType(event), like(String.class, JSON, CUSTOM_ENCODING));
  }

  protected void doInnerAssignmentDataTypePropagationTest(String expression) throws Exception {
    final DataType expectedDataType = DataType.builder().type(Map.class).mediaType(UNKNOWN).charset(CUSTOM_ENCODING).build();

    final Map<String, String> propertyValue = new HashMap<>();
    propertyValue.put(INNER_PROPERTY_NAME, TEST_MESSAGE);
    PrivilegedEvent event = setVariable((PrivilegedEvent) testEvent(), propertyValue, expectedDataType);

    final PrivilegedEvent.Builder builder = PrivilegedEvent.builder(event);
    CompiledExpression compiledExpression = compileMelExpression(expression, event, builder);
    event = builder.build();

    // Attempts to propagate a different dataType, which should be ignored
    dataTypePropagator.propagate(event, builder, new TypedValue<>(propertyValue, DataType.STRING), compiledExpression);
    event = builder.build();

    assertThat(getVariableDataType(event), like(Map.class, UNKNOWN, CUSTOM_ENCODING));
  }

  protected abstract DataType getVariableDataType(PrivilegedEvent event);

  protected abstract PrivilegedEvent setVariable(PrivilegedEvent testEvent, Object propertyValue, DataType expectedDataType);

  private CompiledExpression compileMelExpression(String expression, PrivilegedEvent testEvent, PrivilegedEvent.Builder builder) {
    final ParserConfiguration parserConfiguration = MVELExpressionLanguage.createParserConfiguration(Collections.EMPTY_MAP);
    final MVELExpressionLanguageContext context = createMvelExpressionLanguageContext(testEvent, builder, parserConfiguration);

    CompiledExpression compiledExpression =
        (CompiledExpression) compileExpression(expression, new ParserContext(parserConfiguration));

    // Expression must be executed, otherwise the variable accessor is not properly configured
    MVEL.executeExpression(compiledExpression, context);

    return compiledExpression;
  }

  protected MVELExpressionLanguageContext createMvelExpressionLanguageContext(PrivilegedEvent testEvent,
                                                                              PrivilegedEvent.Builder builder,
                                                                              ParserConfiguration parserConfiguration) {
    final MVELExpressionLanguageContext context = new MVELExpressionLanguageContext(parserConfiguration, muleContext);
    final StaticVariableResolverFactory staticContext = new StaticVariableResolverFactory(parserConfiguration, muleContext);
    final GlobalVariableResolverFactory globalContext =
        new GlobalVariableResolverFactory(emptyMap(), emptyMap(), parserConfiguration, muleContext);

    final DelegateVariableResolverFactory innerDelegate =
        new DelegateVariableResolverFactory(globalContext,
                                            new VariableVariableResolverFactory(parserConfiguration, muleContext, testEvent,
                                                                                builder));
    final DelegateVariableResolverFactory delegate =
        new DelegateVariableResolverFactory(staticContext,
                                            new MessageVariableResolverFactory(parserConfiguration, muleContext, testEvent,
                                                                               builder, innerDelegate));
    context.setNextFactory(new CachedMapVariableResolverFactory(emptyMap(), delegate));
    return context;
  }
}
