/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.mvel2.MVEL.compileExpression;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.transformer.types.MimeTypes.JSON;
import org.mule.api.MuleEvent;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.el.mvel.DelegateVariableResolverFactory;
import org.mule.el.mvel.GlobalVariableResolverFactory;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.el.mvel.MVELExpressionLanguageContext;
import org.mule.el.mvel.MessageVariableResolverFactory;
import org.mule.el.mvel.StaticVariableResolverFactory;
import org.mule.el.mvel.VariableVariableResolverFactory;
import org.mule.mvel2.MVEL;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.mvel2.integration.impl.CachedMapVariableResolverFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

public abstract class AbstractVarAssignmentDataTypePropagatorTestCase extends AbstractMuleContextTestCase
{

    public static final String CUSTOM_ENCODING = StandardCharsets.UTF_16.name();
    public static final String PROPERTY_NAME = "foo";

    private final EnricherDataTypePropagator dataTypePropagator;

    protected AbstractVarAssignmentDataTypePropagatorTestCase(EnricherDataTypePropagator dataTypePropagator)
    {
        this.dataTypePropagator = dataTypePropagator;
    }

    protected void doAssignmentDataTypePropagationTest(PropertyScope scope, String expression) throws Exception
    {
        final DataType expectedDataType = DataTypeFactory.create(String.class, JSON);
        expectedDataType.setEncoding(CUSTOM_ENCODING);

        MuleEvent testEvent = getTestEvent(TEST_MESSAGE);

        final ParserConfiguration parserConfiguration = MVELExpressionLanguage.createParserConfiguration(Collections.EMPTY_MAP);
        final MVELExpressionLanguageContext context = createMvelExpressionLanguageContext(testEvent, parserConfiguration);

        CompiledExpression compiledExpression = (CompiledExpression) compileExpression(expression, new ParserContext(parserConfiguration));
        // Expression must be executed, otherwise the variable accessor is not properly configured
        MVEL.executeExpression(compiledExpression, context);

        dataTypePropagator.propagate(testEvent.getMessage(), new TypedValue(TEST_MESSAGE, expectedDataType), compiledExpression);

        assertThat(testEvent.getMessage().getPropertyDataType(PROPERTY_NAME, scope), like(String.class, JSON, CUSTOM_ENCODING));
    }

    protected MVELExpressionLanguageContext createMvelExpressionLanguageContext(MuleEvent testEvent, ParserConfiguration parserConfiguration)
    {
        final MVELExpressionLanguageContext context = new MVELExpressionLanguageContext(parserConfiguration, muleContext);
        final StaticVariableResolverFactory staticContext = new StaticVariableResolverFactory(parserConfiguration, muleContext);
        final GlobalVariableResolverFactory globalContext = new GlobalVariableResolverFactory(Collections.EMPTY_MAP, Collections.EMPTY_MAP, parserConfiguration, muleContext);

        context.setNextFactory(new CachedMapVariableResolverFactory(Collections.EMPTY_MAP,
                                                                    new DelegateVariableResolverFactory(staticContext, new MessageVariableResolverFactory(
                                                                            parserConfiguration, muleContext, testEvent.getMessage(), new DelegateVariableResolverFactory(
                                                                            globalContext, new VariableVariableResolverFactory(parserConfiguration, muleContext, testEvent))))));
        return context;
    }
}
