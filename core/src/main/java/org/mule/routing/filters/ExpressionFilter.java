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
import org.mule.expression.ExpressionConfig;
import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Allows boolean expressions to be executed on a message. Note that when using this filter you must be able to either specify
 * a boolean expression when using an expression filter or use one of the standard Mule filters.  These can be defined as follows -
 *
 * <ul>
 * <li>RegEx - 'regex:<pattern>': #[regex:'error' [0-9]]</li>
 * <li>Wildcard - 'wildcard:<pattern>': #[wildcard: *foo*</li>
 * <li>PayloadType - 'payload-type:<fully qualified class name>': #[payload:javax.jms.TextMessage]</li>
 * <li>ExceptionType - 'exception-type:<fully qualified class name>': #[exception-type:java.io.FileNotFoundException]</li>
 * <li>Header - 'header:<boolean expression>': #[header:foo!=null]</li>
 * </ul>
 *
 * Otherwise you can use eny expression filter providing you can define a boolean expression i.e.
 * <code>
 * #[xpath:count(/Foo/Bar) == 0]
 * </code>
 *
 * Note that it if the expression is not a boolean expression this filter will return true if the expression returns a result
 * 
 */
public class ExpressionFilter implements Filter, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(ExpressionFilter.class);

    private ExpressionConfig config;
    private String fullExpression;
    private boolean nullReturnsTrue = false;
    private MuleContext muleContext;

    /** For evaluators that are not expression languages we can delegate the execution to another filter */
    private Filter delegateFilter;

    public ExpressionFilter(String evaluator, String customEvaluator, String expression)
    {
        this.config = new ExpressionConfig(expression, evaluator, customEvaluator);
    }

    public ExpressionFilter(String evaluator, String expression)
    {
        this.config = new ExpressionConfig(expression, evaluator, null);
    }

    public ExpressionFilter(String expression)
    {
        this.config = new ExpressionConfig();
        this.config.parse(expression);
    }

    public ExpressionFilter()
    {
        super();
        this.config = new ExpressionConfig();
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

        Object result = muleContext.getExpressionManager().evaluate(expr, message, false);
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
            if(result.toString().toLowerCase().equalsIgnoreCase("false"))
            {
                return false;
            }
            else if(result.toString().toLowerCase().equalsIgnoreCase("true"))
            {
                return true;
            }
            else
            {
                return !nullReturnsTrue;
            }
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
            //Handle non-expression filters
            if(config.getEvaluator().equals("header"))
            {
                delegateFilter = new MessagePropertyFilter(config.getExpression());
            }
            else if(config.getEvaluator().equals("regex"))
            {
                delegateFilter = new RegExFilter(config.getExpression());
            }
            else if(config.getEvaluator().equals("wildcard"))
            {
                delegateFilter = new WildcardFilter(config.getExpression());
            }
            else if(config.getEvaluator().equals("payload-type"))
            {
                try
                {
                    delegateFilter = new PayloadTypeFilter(config.getExpression());
                }
                catch (ClassNotFoundException e)
                {
                    IllegalArgumentException iae = new IllegalArgumentException();
                    iae.initCause(e);
                    throw iae;
                }
            }
            else if(config.getEvaluator().equals("exception-type"))
            {
                try
                {
                    delegateFilter = new ExceptionTypeFilter(config.getExpression());
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
                fullExpression = config.getFullExpression(muleContext.getExpressionManager());
            }
        }
        return fullExpression;
    }

    public String getCustomEvaluator()
    {
        return config.getCustomEvaluator();
    }

    public void setCustomEvaluator(String customEvaluator)
    {
        this.config.setCustomEvaluator(customEvaluator);
        fullExpression=null;        
    }

    public String getEvaluator()
    {
        return config.getEvaluator();
    }

    public void setEvaluator(String evaluator)
    {
        this.config.setEvaluator(evaluator);
        fullExpression=null;
    }

    public String getExpression()
    {
        return config.getExpression();
    }

    public void setExpression(String expression)
    {
        this.config.setExpression(expression);
        fullExpression=null;
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
        return equal(config, other.config)
            && equal(delegateFilter, other.delegateFilter)
            && nullReturnsTrue == other.nullReturnsTrue;
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), config, delegateFilter, nullReturnsTrue});
    }
}
