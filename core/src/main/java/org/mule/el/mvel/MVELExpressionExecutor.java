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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MVELExpressionExecutor implements ExpressionExecutor<MVELExpressionLanguageContext>
{

    private static Logger log = LoggerFactory.getLogger(MVELExpressionExecutor.class);

    protected static final int COMPILED_EXPRESSION_MAX_CACHE_SIZE = 1000;

    protected ParserConfiguration parserConfiguration;

    protected Cache<String, Serializable> compiledExpressionsCache = CacheBuilder.newBuilder()
        .maximumSize(COMPILED_EXPRESSION_MAX_CACHE_SIZE)
        .build();

    public MVELExpressionExecutor(ParserConfiguration parserConfiguration)
    {
        this.parserConfiguration = parserConfiguration;
        System.setProperty("mvel2.compiler.allow_override_all_prophandling", "true");
        // Use reflective optimizer rather than default to avoid concurrency issues with JIT complication.
        // See MULE-6630
        OptimizerFactory.setDefaultOptimizer("ASM");
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
            return compiledExpressionsCache.get(expression, new Callable<Serializable>()
            {
                @Override
                public Serializable call()
                {
                    return MVEL.compileExpression(expression, new ParserContext(parserConfiguration));
                }
            });
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
        catch (ExecutionException e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}
