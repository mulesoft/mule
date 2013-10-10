/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
