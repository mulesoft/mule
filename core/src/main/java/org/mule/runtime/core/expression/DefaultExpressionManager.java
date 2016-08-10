/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.expression;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.expression.InvalidExpressionException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.TemplateParser;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides universal access for evaluating expressions embedded in Mule configurations, such as Xml, Java,
 * scripting and annotations.
 */
public class DefaultExpressionManager implements ExpressionManager, MuleContextAware, Initialisable
{

    /**
     * logger used by this class
     */
    protected static transient final Logger logger = LoggerFactory.getLogger(DefaultExpressionManager.class);

    public static final String OBJECT_FOR_ENRICHMENT = "__object_for_enrichment";

    // default style parser
    private TemplateParser parser = TemplateParser.createMuleStyleParser();


    private MuleContext muleContext;

    private ExpressionLanguage expressionLanguage;

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public Object evaluate(String expression, MuleEvent event) throws ExpressionRuntimeException
    {
        return evaluate(expression, event, false);
    }

    @Override
    public Object evaluate(String expression, MuleEvent event, boolean failIfNull)
        throws ExpressionRuntimeException
    {
        expression = removeExpressionMarker(expression);
        return expressionLanguage.evaluate(expression, event);
    }

    @Override
    public void enrich(String expression, MuleEvent event, Object object)
    {
        expression = removeExpressionMarker(expression);
        expression = createEnrichmentExpression(expression);
        expressionLanguage.evaluate(expression, event,
                                    Collections.singletonMap(OBJECT_FOR_ENRICHMENT, object));
    }

    @Override
    public void enrichTyped(String expression, MuleEvent event, TypedValue object)
    {
        expression = removeExpressionMarker(expression);
        expressionLanguage.enrich(createEnrichmentExpression(expression), event, object);
    }

    @Override
    public boolean evaluateBoolean(String expression,
                                   MuleEvent event,
                                   boolean nullReturnsTrue,
                                   boolean nonBooleanReturnsTrue) throws ExpressionRuntimeException
    {
        return resolveBoolean(evaluate(expression, event, false), nullReturnsTrue, nonBooleanReturnsTrue,
            expression);
    }

    protected boolean resolveBoolean(Object result,
                                     boolean nullReturnsTrue,
                                     boolean nonBooleanReturnsTrue,
                                     String expression)
    {
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
            if (result.toString().toLowerCase().equalsIgnoreCase("false"))
            {
                return false;
            }
            else if (result.toString().toLowerCase().equalsIgnoreCase("true"))
            {
                return true;
            }
            else
            {
                return nonBooleanReturnsTrue;
            }
        }
        else
        {
            logger.warn("Expression: " + expression + ", returned an non-boolean result. Returning: "
                        + nonBooleanReturnsTrue);
            return nonBooleanReturnsTrue;
        }
    }

    @Override
    public String parse(String expression, MuleEvent event) throws ExpressionRuntimeException
    {
        return parse(expression, event, false);
    }

    @Override
    public String parse(String expression, final MuleEvent event, final boolean failIfNull)
        throws ExpressionRuntimeException
    {
        return parser.parse(new TemplateParser.TemplateCallback()
        {
            @Override
            public Object match(String token)
            {
                Object result = evaluate(token, event, failIfNull);
                if (result instanceof MuleMessage)
                {
                    return ((MuleMessage) result).getPayload();
                }
                else
                {
                    return result;
                }
            }
        }, expression);
    }

    @Override
    public TypedValue evaluateTyped(String expression, MuleEvent event)
    {
        expression = removeExpressionMarker(expression);
        return expressionLanguage.evaluateTyped(expression, event);
    }

    @Override
    public boolean isExpression(String expression)
    {
        return (expression.contains(DEFAULT_EXPRESSION_PREFIX));
    }

    @Override
    public boolean isValidExpression(String expression)
    {
        try
        {
            validateExpression(expression);
            return true;
        }
        catch (InvalidExpressionException e)
        {
            logger.warn(e.getMessage());
            return false;
        }
    }

    @Override
    public void validateExpression(String expression) throws InvalidExpressionException
    {
        if (!muleContext.getConfiguration().isValidateExpressions())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Validate expressions is turned off, no checking done for: " + expression);
            }
            return;
        }
        try
        {
            parser.validate(expression);
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidExpressionException(expression, e.getMessage());
        }

        final AtomicBoolean valid = new AtomicBoolean(true);
        final AtomicBoolean match = new AtomicBoolean(false);
        final StringBuilder message = new StringBuilder();
        parser.parse(token -> {
            match.set(true);
            if (valid.get())
            {
                try
                {
                    expressionLanguage.validate(token);
                }
                catch (InvalidExpressionException e)
                {
                    valid.compareAndSet(true, false);
                    message.append(token).append(" is invalid\n");
                    message.append(e.getMessage());
                }
            }
            return null;
        }, expression);

        if (message.length() > 0)
        {
            throw new InvalidExpressionException(expression, message.toString());
        }
        else if (!match.get())
        {
            throw new InvalidExpressionException(expression,
                "Expression string is not an expression.  Use isExpression(String) to validate first");
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        expressionLanguage = muleContext.getExpressionLanguage();
    }

    @Override
    public boolean evaluateBoolean(String expression, MuleEvent event) throws ExpressionRuntimeException
    {
        return evaluateBoolean(expression, event, false, false);
    }

    public static String removeExpressionMarker(String expression)
    {
        if (expression == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
        }
        if (expression.startsWith(DEFAULT_EXPRESSION_PREFIX))
        {
            expression = expression.substring(2, expression.length() - 1);
        }
        return expression;
    }

    protected String createEnrichmentExpression(String expression)
    {
        if (expression.contains("$"))
        {
            expression = StringUtils.replace(expression, "$", OBJECT_FOR_ENRICHMENT);
        }
        else
        {
            expression = expression + "=" + OBJECT_FOR_ENRICHMENT;
        }
        return expression;
    }

    public void setExpressionLanguage(ExpressionLanguage expressionLanguage)
    {
        this.expressionLanguage = expressionLanguage;
    }

    public ExpressionLanguage getExpressionLanguage()
    {
        return expressionLanguage;
    }
}
