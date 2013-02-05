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

import org.mule.api.el.ExpressionExecutor;
import org.mule.api.expression.InvalidExpressionException;

import java.io.Serializable;

import org.apache.commons.collections.map.LRUMap;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.optimizers.OptimizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MVELExpressionExecutor implements ExpressionExecutor<MVELExpressionLanguageContext>
{

    private static Logger log = LoggerFactory.getLogger(MVELExpressionExecutor.class);

    protected static final int COMPILED_EXPRESSION_MAX_CACHE_SIZE = 1000;

    protected ParserContext parserContext;
    protected LRUMap compiledExpressionsCache = new LRUMap(COMPILED_EXPRESSION_MAX_CACHE_SIZE);

    public MVELExpressionExecutor(ParserContext parserContext)
    {
        this.parserContext = parserContext;
    }

    public Object execute(String expression, MVELExpressionLanguageContext context)
    {
        // Use reflective optimizer rather than default to avoid concurrency issues with JIT complication.
        // See MULE-6630
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);

        if (log.isTraceEnabled())
        {
            log.trace("Executing MVEL expression '" + expression + "' with context: \n" + context.toString());
        }
        return MVEL.executeExpression(getCompiledExpression(expression), context);
    }

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

}
