/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.CoreMessages;
import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Allows boolean expressions to be executed on a message
 */
public class ExpressionFilter implements Filter, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(ExpressionFilter.class);

    private String evaluator;
    private String expression;
    private String customEvaluator;
    private String fullExpression;
    private boolean nullReturnsTrue = false;
    private MuleContext muleContext;

    private static final String TRUE = "true";

    /** For evaluators that are not expression languages we can delegate the execution to another filter */
    private Filter delegateFilter;

    public ExpressionFilter(String evaluator, String customEvaluator, String expression)
    {
        this.customEvaluator = customEvaluator;
        this.evaluator = evaluator;
        this.expression = expression;
    }

    public ExpressionFilter(String evaluator, String expression)
    {
        this.evaluator = evaluator;
        this.expression = expression;
    }

    public ExpressionFilter(String expression)
    {
        int i = expression.indexOf(":");
        if(i < 0)
        {
            throw new IllegalArgumentException("Expression is invalid: " + expression);
        }
        this.evaluator = expression.substring(0, i);
        this.expression = expression.substring(i+1);
    }

    public ExpressionFilter()
    {
        super();
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    /**
     * Check a given message against this filter.
     *
     * @param message a non null message to filter.
     * @return <code>true</code> if the message matches the filter
     */
    public boolean accept(MuleMessage message)
    {
        String expr = getFullExpression();
        if (delegateFilter != null)
        {
            boolean result = delegateFilter.accept(message);
            if (logger.isDebugEnabled())
            {
                logger.debug(MessageFormat.format("Result of expression filter: {0} is: {1}", expr, result));
            }
            return result;
        }

        Object result = muleContext.getExpressionManager().evaluate(expr, message);
        if (result == null)
        {
            return nullReturnsTrue;
        }
        else if (result instanceof Boolean)
        {
            return (Boolean) result;
        }
        else if (result instanceof String)
        {
            return ((String) result).toLowerCase().equals(TRUE);
        }
        else
        {
            logger.warn("Expression: " + expr + ", returned an non-boolean result. Returning: "
                        + !nullReturnsTrue);
            return !nullReturnsTrue;
        }
    }

    protected String getFullExpression()
    {
        if(fullExpression==null)
        {
            if(evaluator==null)
            {
                throw new IllegalArgumentException(CoreMessages.objectIsNull("evaluator").getMessage());
            }
            if(evaluator.equals("custom"))
            {
                if(customEvaluator==null)
                {
                    throw new IllegalArgumentException(CoreMessages.objectIsNull("customEvaluator").getMessage());
                }
                else
                {
                    evaluator = customEvaluator;
                }
            }
            if(evaluator.equals("header"))
            {
                delegateFilter = new MessagePropertyFilter(expression);
            }
            else if(evaluator.equals("regex"))
            {
                delegateFilter = new RegExFilter(expression);
            }
            else if(evaluator.equals("wildcard"))
            {
                delegateFilter = new WildcardFilter(expression);
            }
            else if(evaluator.equals("payload-type"))
            {
                try
                {
                    delegateFilter = new PayloadTypeFilter(expression);
                }
                catch (ClassNotFoundException e)
                {
                    IllegalArgumentException iae = new IllegalArgumentException();
                    iae.initCause(e);
                    throw iae;
                }
            }
            else if(evaluator.equals("exception-type"))
            {
                try
                {
                    delegateFilter = new ExceptionTypeFilter(expression);
                }
                catch (ClassNotFoundException e)
                {
                    IllegalArgumentException iae = new IllegalArgumentException();
                    iae.initCause(e);
                    throw iae;
                }
            }
            else
            {
                //In the case of 'payload' the expression can be null
                fullExpression = evaluator + ":" + (expression==null ? "" : expression);
            }
        }
        return fullExpression;
    }

    public String getCustomEvaluator()
    {
        return customEvaluator;
    }

    public void setCustomEvaluator(String customEvaluator)
    {
        this.customEvaluator = customEvaluator;
    }

    public String getEvaluator()
    {
        return evaluator;
    }

    public void setEvaluator(String evaluator)
    {
        this.evaluator = evaluator;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public boolean isNullReturnsTrue()
    {
        return nullReturnsTrue;
    }

    public void setNullReturnsTrue(boolean nullReturnsTrue)
    {
        this.nullReturnsTrue = nullReturnsTrue;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final ExpressionFilter other = (ExpressionFilter) obj;
        return equal(customEvaluator, other.customEvaluator)
            && equal(delegateFilter, other.delegateFilter)
            && equal(evaluator, other.evaluator)
            && equal(expression, other.expression)
            && equal(fullExpression, other.fullExpression)
            && nullReturnsTrue == other.nullReturnsTrue;
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), customEvaluator, delegateFilter,
            evaluator, expression, fullExpression, nullReturnsTrue});
    }
}
