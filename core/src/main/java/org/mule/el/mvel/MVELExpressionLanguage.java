/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.LRUMap;
import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.CachedMapVariableResolverFactory;

/**
 * Expression language that uses MVEL (http://mvel.codehaus.org/).
 */
public class MVELExpressionLanguage implements ExpressionLanguage, Initialisable, Disposable
{

    private static final int COMPILED_EXPRESSION_MAX_CACHE_SIZE = 1000;

    protected ParserContext parserContext;
    protected LRUMap compiledExpressionsCache = new LRUMap(COMPILED_EXPRESSION_MAX_CACHE_SIZE);
    protected AppVariableResolverFactory appVariableResolverFactory;
    protected MuleContext muleContext;

    public MVELExpressionLanguage(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        System.setProperty("mvel2.compiler.allow_override_all_prophandling", "true");

        parserContext = createParserContext();

        appVariableResolverFactory = new AppVariableResolverFactory(parserContext, muleContext);
    }

    @Override
    public void dispose()
    {
        compiledExpressionsCache.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression)
    {
        return (T) interalExecuteExpression(expression, appVariableResolverFactory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression, Map<String, Object> vars)
    {
        VariableResolverFactoryChainBuilder builder = new VariableResolverFactoryChainBuilder(
            appVariableResolverFactory);
        builder.add(new CachedMapVariableResolverFactory(vars));
        return (T) interalExecuteExpression(expression, builder.build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression, MuleEvent event)
    {
        return (T) evaluate(expression, event, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression, MuleEvent event, Map<String, Object> vars)
    {
        VariableResolverFactoryChainBuilder builder = new VariableResolverFactoryChainBuilder(
            appVariableResolverFactory);
        builder.add(new CachedMapVariableResolverFactory(vars));
        return (T) interalExecuteExpression(expression, builder.build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression, MuleMessage message)
    {
        return (T) interalExecuteExpression(expression, appVariableResolverFactory);
    }

    @SuppressWarnings("unchecked")
    protected <T> T interalExecuteExpression(String expression,
                                             VariableResolverFactory variableResolverFactory)
    {
        try
        {
            return (T) MVEL.executeExpression(getCompiledExpression(expression), variableResolverFactory);
        }
        catch (Exception e)
        {
            throw new ExpressionRuntimeException(CoreMessages.expressionEvaluationFailed(expression), e);
        }

    }

    @Override
    public boolean isValid(String expression)
    {
        try
        {
            getCompiledExpression(expression);
            return true;
        }
        catch (CompileException e)
        {
            return false;
        }
    }

    protected ParserContext createParserContext()
    {
        ParserContext parserContext = new ParserContext();
        configureParserContext(parserContext);
        return parserContext;
    }

    protected void configureParserContext(ParserContext parserContext)
    {
        // defaults importsw
        parserContext.addImport(Date.class);
        parserContext.addImport(Collection.class);
        parserContext.addImport(List.class);
        parserContext.addImport(Map.class);
        parserContext.addImport(Set.class);
        parserContext.addImport(Boolean.class);
        parserContext.addImport(Byte.class);
        parserContext.addImport(Character.class);
        parserContext.addImport(Float.class);
        parserContext.addImport(Enum.class);
        parserContext.addImport(Integer.class);
        parserContext.addImport(Long.class);
        parserContext.addImport(Math.class);
        parserContext.addImport(Number.class);
        parserContext.addImport(Object.class);
        parserContext.addImport(Short.class);
        parserContext.addImport(String.class);
        parserContext.addImport(System.class);
        parserContext.addImport(Calendar.class);
    }

    /**
     * Compile an expression. If such expression was compiled before then return the compilation output from a
     * cache.
     * 
     * @param expression Expression to be compiled
     * @return A {@link Serializable} object representing the compiled expression
     */
    protected Serializable getCompiledExpression(String expression)
    {
        if (compiledExpressionsCache.containsKey(expression))
        {
            return (Serializable) compiledExpressionsCache.get(expression);
        }
        else
        {
            Serializable compiledExpression = MVEL.compileExpression(expression, parserContext);
            compiledExpressionsCache.put(expression, compiledExpression);
            return compiledExpression;
        }
    }

    /**
     * Utility builder for creating chain of {@link VariableResolverFactory}'s
     */
    private class VariableResolverFactoryChainBuilder
    {

        List<VariableResolverFactory> factories = new ArrayList<VariableResolverFactory>();

        public VariableResolverFactoryChainBuilder(VariableResolverFactory factory)
        {
            add(factory);
        }

        public VariableResolverFactoryChainBuilder add(VariableResolverFactory factory)
        {
            factories.add(factory);
            return this;
        }

        public VariableResolverFactory build()
        {
            for (int i = 0; i < factories.size() - 1; i++)
            {
                factories.get(i).setNextFactory(factories.get(i + 1));
            }
            return factories.get(0);
        }
    }

}
