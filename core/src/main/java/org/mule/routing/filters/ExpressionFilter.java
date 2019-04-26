/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static org.mule.api.config.MuleProperties.MULE_EXPRESSION_FILTER_DEFAULT_BOOLEAN_VALUE;
import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transport.PropertyScope;
import org.mule.expression.ExceptionTypeExpressionEvaluator;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.PayloadTypeExpressionEvaluator;
import org.mule.expression.RegexExpressionEvaluator;
import org.mule.expression.WilcardExpressionEvaluator;
import org.mule.util.StringUtils;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Allows boolean expressions to be executed on a message. Note that when using this filter you must be able
 * to either specify a boolean expression when using an expression filter or use one of the standard Mule
 * filters. These can be defined as follows -
 * <ul>
 * <li>RegEx - 'regex:<pattern>': #[regex:'error' [0-9]]</li>
 * <li>Wildcard - 'wildcard:<pattern>': #[wildcard: *foo*</li>
 * <li>PayloadType - 'payload-type:<fully qualified class name>': #[payload:javax.jms.TextMessage]</li>
 * <li>ExceptionType - 'exception-type:<fully qualified class name>':
 * #[exception-type:java.io.FileNotFoundException]</li>
 * <li>Header - 'header:<boolean expression>': #[header:foo!=null]</li>
 * </ul>
 * Otherwise you can use eny expression filter providing you can define a boolean expression i.e. <code>
 * #[xpath:count(/Foo/Bar) == 0]
 * </code> Note that it if the expression is not a boolean expression this filter will return true if the
 * expression returns a result
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
    private boolean nonBooleanReturnsTrue = parseBoolean(getProperty(MULE_EXPRESSION_FILTER_DEFAULT_BOOLEAN_VALUE, "true"));
    private MuleContext muleContext;

    /**
     * The class-loader that should be used to load any classes used in scripts. Default to the classloader
     * used to load this filter
     **/
    private ClassLoader expressionEvaluationClassLoader = Thread.currentThread().getContextClassLoader();

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

        // MULE-4797 Because there is no way to specify the class-loader that script
        // engines use and because scripts when used for expressions are compiled in
        // runtime rather than at initialization the only way to ensure the correct
        // class-loader to used is to switch it out here. We may want to consider
        // passing the class-loader to the ExpressionManager and only doing this for
        // certain ExpressionEvaluators further in.
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(expressionEvaluationClassLoader);
            return muleContext.getExpressionManager().evaluateBoolean(expr, message, nullReturnsTrue, nonBooleanReturnsTrue);
        }
        finally
        {
            // Restore original context class-loader
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    protected String getFullExpression()
    {
        if (config.getEvaluator() == null)
        {
            return config.getExpression();
        }
        if (fullExpression == null)
        {
            // Handle non-expression filters
            if (config.getEvaluator().equals("header"))
            {
                delegateFilter = new MessagePropertyFilter(config.getExpression());
            }
            else if (config.getEvaluator().equals("variable"))
            {
                delegateFilter = new MessagePropertyFilter(PropertyScope.INVOCATION_NAME + ":"
                                                           + config.getExpression());
            }
            else if (config.getEvaluator().equals(RegexExpressionEvaluator.NAME))
            {
                delegateFilter = new RegExFilter(config.getExpression());
            }
            else if (config.getEvaluator().equals(WilcardExpressionEvaluator.NAME))
            {
                delegateFilter = new WildcardFilter(config.getExpression());
            }
            else if (config.getEvaluator().equals(PayloadTypeExpressionEvaluator.NAME))
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
            else if (config.getEvaluator().equals(ExceptionTypeExpressionEvaluator.NAME))
            {
                try
                {
                    if (StringUtils.isEmpty(config.getExpression()))
                    {
                        delegateFilter = new ExceptionTypeFilter();
                    }
                    else
                    {
                        delegateFilter = new ExceptionTypeFilter(config.getExpression());
                    }
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
                // In the case of 'payload' the expression can be null
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
        fullExpression = null;
    }

    public String getEvaluator()
    {
        return config.getEvaluator();
    }

    public void setEvaluator(String evaluator)
    {
        this.config.setEvaluator(evaluator);
        fullExpression = null;
    }

    public String getExpression()
    {
        return config.getExpression();
    }

    public void setExpression(String expression)
    {
        this.config.setExpression(expression);
        fullExpression = null;
    }

    public boolean isNullReturnsTrue()
    {
        return nullReturnsTrue;
    }

    public void setNullReturnsTrue(boolean nullReturnsTrue)
    {
        this.nullReturnsTrue = nullReturnsTrue;
    }

    public void setNonBooleanReturnsTrue(boolean nonBooleanReturnsTrue)
    {
        this.nonBooleanReturnsTrue = nonBooleanReturnsTrue;
    }

    public boolean isNonBooleanReturnsTrue()
    {
        return nonBooleanReturnsTrue;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final ExpressionFilter other = (ExpressionFilter) obj;
        return equal(config, other.config) && equal(delegateFilter, other.delegateFilter)
               && nullReturnsTrue == other.nullReturnsTrue;
    }

    @Override
    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), config, delegateFilter, nullReturnsTrue});
    }
}
