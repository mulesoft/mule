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

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Test;

public abstract class AbstractVarExpressionDataTypeResolverTestCase extends AbstractMuleContextTestCase
{

    public static final String EXPRESSION_VALUE = "bar";
    public static final String CUSTOM_ENCODING = StandardCharsets.UTF_16.name();
    public static final String PROPERTY_NAME = "foo";

    private final ExpressionDataTypeResolver expressionDataTypeResolver;
    private final PropertyScope scope;
    private final String variableName;

    protected AbstractVarExpressionDataTypeResolverTestCase(ExpressionDataTypeResolver expressionDataTypeResolver, PropertyScope scope, String variableName)
    {
        this.expressionDataTypeResolver = expressionDataTypeResolver;
        this.scope = scope;
        this.variableName = variableName;
    }

    @Test
    public void returnsFlowVarDataTypeUsingMapSyntax() throws Exception
    {
        doVarDataTypeTest(variableName + "['" + PROPERTY_NAME + "']");
    }

    @Test
    public void returnsFlowVarDataTypeUsingDotSyntax() throws Exception
    {
        doVarDataTypeTest(variableName + "." + PROPERTY_NAME );
    }

    @Test
    public void returnsFlowVarDataTypeUsingEscapedDotSyntax() throws Exception
    {
        doVarDataTypeTest(variableName + ".'" + PROPERTY_NAME + "'");
    }

    protected void doVarDataTypeTest(String expression) throws Exception
    {
        final DataType expectedDataType = DataTypeFactory.create(String.class, JSON);
        expectedDataType.setEncoding(CUSTOM_ENCODING);

        MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
        testEvent.getMessage().setProperty(PROPERTY_NAME, EXPRESSION_VALUE, scope, expectedDataType);

        final ParserConfiguration parserConfiguration = MVELExpressionLanguage.createParserConfiguration(Collections.EMPTY_MAP);
        final MVELExpressionLanguageContext context = createMvelExpressionLanguageContext(testEvent, parserConfiguration);

        CompiledExpression compiledExpression = (CompiledExpression) compileExpression(expression, new ParserContext(parserConfiguration));
        // Expression must be executed, otherwise the variable accessor is not properly configured
        MVEL.executeExpression(compiledExpression, context);

        assertThat(expressionDataTypeResolver.resolve(testEvent.getMessage(), compiledExpression), like(String.class, JSON, CUSTOM_ENCODING));
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
