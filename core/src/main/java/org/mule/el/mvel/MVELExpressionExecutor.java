/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.api.MuleRuntimeException;
import org.mule.api.el.ExpressionExecutor;
import org.mule.api.expression.InvalidExpressionException;
import org.mule.mvel2.MVEL;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.optimizers.OptimizerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MVELExpressionExecutor implements ExpressionExecutor<MVELExpressionLanguageContext>
{

    private static Logger log = LoggerFactory.getLogger(MVELExpressionExecutor.class);

    protected static final int COMPILED_EXPRESSION_MAX_CACHE_SIZE = 1000;

    protected ParserConfiguration parserConfiguration;

    protected LoadingCache<String, Serializable> compiledExpressionsCache;

    public MVELExpressionExecutor(final ParserConfiguration parserConfiguration)
    {
        this.parserConfiguration = parserConfiguration;

        MVEL.COMPILER_OPT_PROPERTY_ACCESS_DOESNT_FAIL = true;

        compiledExpressionsCache = CacheBuilder.newBuilder()
            .maximumSize(COMPILED_EXPRESSION_MAX_CACHE_SIZE)
            .build(new CacheLoader<String, Serializable>()
            {
                @Override
                public Serializable load(String key) throws Exception
                {
                    return MVEL.compileExpression(key, new ParserContext(parserConfiguration));
                }
            });
    }

    @Override
    public Object execute(String expression, MVELExpressionLanguageContext context)
    {
        if (log.isTraceEnabled())
        {
            log.trace("Executing MVEL expression '" + expression + "' with context: \n" + context.toString());
        }
        return MVEL.executeExpression(getCompiledExpression(expression), context);
    }

    @Override
    public void validate(String expression) throws InvalidExpressionException
    {
        getCompiledExpression(expression);
    }

    /**
     * Compile an expression. If such expression was compiled before then return the compilation output from a
     * cache.
     * 
     * @param expression Expression to be compiled
     * @return A {@link Serializable} object representing the compiled expression
     */
    protected Serializable getCompiledExpression(final String expression)
    {
        try
        {
            return compiledExpressionsCache.getUnchecked(expression);
        }
        catch (UncheckedExecutionException e)
        {
            // While exception is called UncheckedExecutionException and it generally wraps a RuntimeException
            // only the javadoc states that a non-runtime exception is also possible.
            if (e.getCause() instanceof RuntimeException)
            {
                throw (RuntimeException) e.getCause();
            }
            else
            {
                throw new MuleRuntimeException(e);
            }
        }
    }
}
