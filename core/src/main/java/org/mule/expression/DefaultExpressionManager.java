/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.expression.ExpressionEnricher;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.expression.InvalidExpressionException;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.transformer.types.TypedValue;
import org.mule.transport.NullPayload;
import org.mule.util.StringUtils;
import org.mule.util.TemplateParser;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides universal access for evaluating expressions embedded in Mule configurations, such as Xml, Java,
 * scripting and annotations.
 * <p/>
 * Users can register or unregister {@link ExpressionEvaluator} through this interface.
 */
public class DefaultExpressionManager implements ExpressionManager, MuleContextAware, Initialisable
{

    /**
     * logger used by this class
     */
    protected static transient final Log logger = LogFactory.getLog(DefaultExpressionManager.class);

    public static final String OBJECT_FOR_ENRICHMENT = "__object_for_enrichment";

    // default style parser
    private TemplateParser parser = TemplateParser.createMuleStyleParser();

    private ConcurrentMap evaluators = new ConcurrentHashMap(8);
    private ConcurrentMap enrichers = new ConcurrentHashMap(8);

    private MuleContext muleContext;

    private ExpressionLanguage expressionLanguage;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void registerEvaluator(ExpressionEvaluator evaluator)
    {
        if (evaluator == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("evaluator").getMessage());
        }

        final String name = evaluator.getName();
        // TODO MULE-3809 Eliminate duplicate evaluators registration
        if (logger.isDebugEnabled())
        {
            logger.debug("Evaluators already contain an object named '" + name
                         + "'.  The previous object will be overwritten.");
        }
        evaluators.put(evaluator.getName(), evaluator);
    }

    public void registerEnricher(ExpressionEnricher enricher)
    {
        if (enricher == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("enricher").getMessage());
        }

        final String name = enricher.getName();
        // TODO MULE-3809 Eliminate duplicate evaluators registration
        if (logger.isDebugEnabled())
        {
            logger.debug("Enrichers already contain an object named '" + name
                         + "'.  The previous object will be overwritten.");
        }
        enrichers.put(enricher.getName(), enricher);
    }

    /**
     * Checks whether an evaluator is registered with the manager
     * 
     * @param name the name of the expression evaluator
     * @return true if the evaluator is registered with the manager, false otherwise
     */
    public boolean isEvaluatorRegistered(String name)
    {
        return name != null ? evaluators.containsKey(name) : false;
    }

    /**
     * Checks whether an enricher is registered with the manager
     * 
     * @param name the name of the expression enricher
     * @return true if the enricher is registered with the manager, false otherwise
     */
    public boolean isEnricherRegistered(String name)
    {
        return name != null ? enrichers.containsKey(name) : false;
    }

    /**
     * Removes the evaluator with the given name
     * 
     * @param name the name of the evaluator to remove
     */
    public ExpressionEvaluator unregisterEvaluator(String name)
    {
        if (name == null)
        {
            return null;
        }

        ExpressionEvaluator evaluator = (ExpressionEvaluator) evaluators.remove(name);
        if (evaluator instanceof Disposable)
        {
            ((Disposable) evaluator).dispose();
        }
        return evaluator;
    }

    /**
     * Removes the evaluator with the given name
     * 
     * @param name the name of the evaluator to remove
     */
    public ExpressionEnricher unregisterEnricher(String name)
    {
        if (name == null)
        {
            return null;
        }

        ExpressionEnricher enricher = (ExpressionEnricher) enrichers.remove(name);
        if (enricher instanceof Disposable)
        {
            ((Disposable) enricher).dispose();
        }
        return enricher;
    }

    /**
     * Evaluates the given expression. The expression should be a single expression definition with or without
     * enclosing braces. i.e. "context:serviceName" and "#[context:serviceName]" are both valid. For
     * situations where one or more expressions need to be parsed within a single text, the
     * {@link org.mule.api.expression.ExpressionManager#parse(String,org.mule.api.MuleMessage,boolean)} method
     * should be used since it will iterate through all expressions in a string.
     * 
     * @param expression a single expression i.e. xpath://foo
     * @param message the current message to process. The expression will evaluata on the message.
     * @return the result of the evaluation. Expressions that return collection will return an empty
     *         collection, not null.
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression
     *             and 'failIfNull is set to true.
     */
    public Object evaluate(String expression, MuleMessage message) throws ExpressionRuntimeException
    {
        return evaluate(expression, message, false);
    }

    public Object evaluate(String expression, MuleEvent event) throws ExpressionRuntimeException
    {
        return evaluate(expression, event, false);
    }

    /**
     * Evaluates the given expression. The expression should be a single expression definition with or without
     * enclosing braces. i.e. "context:serviceName" and "#[context:serviceName]" are both valid. For
     * situations where one or more expressions need to be parsed within a single text, the
     * {@link org.mule.api.expression.ExpressionManager#parse(String,org.mule.api.MuleMessage,boolean)} method
     * should be used since it will iterate through all expressions in a string.
     * 
     * @param expression a single expression i.e. xpath://foo
     * @param message the current message to process. The expression will evaluata on the message.
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or
     *            returns null.
     * @return the result of the evaluation. Expressions that return collection will return an empty
     *         collection, not null.
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression
     *             and 'failIfNull is set to true.
     */
    public Object evaluate(String expression, MuleEvent event, boolean failIfNull)
        throws ExpressionRuntimeException
    {
        expression = removeExpressionMarker(expression);
        if (isEvaluatorExpression(expression))
        {
            return evaluate(expression, event.getMessage(), failIfNull);
        }
        else
        {
            return expressionLanguage.evaluate(expression, event);
        }
    }

    public Object evaluate(String expression, MuleMessage message, boolean failIfNull)
    {
        expression = removeExpressionMarker(expression);
        if (isEvaluatorExpression(expression))
        {
            String[] parts = expression.split(":", 2);
            return evaluate(parts[1], parts[0], message, failIfNull);
        }
        else
        {
            return expressionLanguage.evaluate(expression, message);
        }
    }

    public void enrich(String expression, MuleMessage message, Object object)
        throws ExpressionRuntimeException
    {
        expression = removeExpressionMarker(expression);
        if (isEvaluatorExpression(expression))
        {
            String[] parts = expression.split(":", 2);
            enrich(parts[1], parts[0], message, object);
        }
        else
        {

            expressionLanguage.evaluate(createEnrichmentExpression(expression), message,
                                        Collections.singletonMap(OBJECT_FOR_ENRICHMENT, object));
        }
    }

    @Override
    public void enrichTyped(String expression, MuleMessage message, TypedValue object)
    {
        expression = removeExpressionMarker(expression);
        if (isEvaluatorExpression(expression))
        {
            String[] parts = expression.split(":", 2);
            enrich(parts[1], parts[0], message, object.getValue());
        }
        else
        {
            expressionLanguage.enrich(createEnrichmentExpression(expression), message, object);
        }
    }

    @Override
    public void enrich(String expression, MuleEvent event, Object object)
    {
        expression = removeExpressionMarker(expression);
        if (isEvaluatorExpression(expression))
        {
            enrich(expression, event.getMessage(), object);
        }
        else
        {
            expression = createEnrichmentExpression(expression);
            expressionLanguage.evaluate(expression, event,
                                        Collections.singletonMap(OBJECT_FOR_ENRICHMENT, object));
        }
    }

    public void enrich(String expression, String enricherName, MuleMessage message, Object object)
    {
        ExpressionEnricher enricher = (ExpressionEnricher) enrichers.get(enricherName);
        if (enricher == null)
        {
            throw new IllegalArgumentException(CoreMessages.expressionEnricherNotRegistered(enricherName)
                .getMessage());
        }
        enricher.enrich(expression, message, object);
    }

    /**
     * Evaluates the given expression. The expression should be a single expression definition with or without
     * enclosing braces. i.e. "context:serviceName" and "#[context:serviceName]" are both valid. For
     * situations where one or more expressions need to be parsed within a single text, the
     * {@link org.mule.api.expression.ExpressionManager#parse(String,org.mule.api.MuleMessage,boolean)} method
     * should be used since it will iterate through all expressions in a string.
     * 
     * @param expression a single expression i.e. xpath://foo
     * @param evaluator the evaluator to use when executing the expression
     * @param message the current message to process. The expression will evaluata on the message.
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or
     *            returns null or if an exception should be thrown if an empty collection is returned.
     * @return the result of the evaluation. Expressions that return collection will return an empty
     *         collection, not null.
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression
     *             and 'failIfNull is set to true.
     */
    public Object evaluate(String expression, String evaluator, MuleMessage message, boolean failIfNull)
        throws ExpressionRuntimeException
    {
        if (evaluator == null)
        {
            return expressionLanguage.evaluate(expression, message);
        }
        ExpressionEvaluator extractor = getExpressionEvaluator(evaluator);
        Object result = extractor.evaluate(expression, message);
        checkRequiredValue(expression, evaluator, failIfNull, result);
        return result;
    }

    @Override
    public TypedValue evaluateTyped(String expression, String evaluator, MuleMessage message, boolean failIfNull)
            throws ExpressionRuntimeException
    {
        if (evaluator == null)
        {
            return expressionLanguage.evaluateTyped(expression, message);
        }
        ExpressionEvaluator extractor = getExpressionEvaluator(evaluator);

        TypedValue result = extractor.evaluateTyped(expression, message);

        checkRequiredValue(expression, evaluator, failIfNull, result.getValue());

        return result;
    }

    private ExpressionEvaluator getExpressionEvaluator(String evaluator)
    {
        ExpressionEvaluator extractor = (ExpressionEvaluator) evaluators.get(evaluator);

        if (extractor == null)
        {
            throw new IllegalArgumentException(CoreMessages.expressionEvaluatorNotRegistered(evaluator)
                                                       .getMessage());
        }

        return extractor;
    }

    private void checkRequiredValue(String expression, String evaluator, boolean failIfNull, Object value)
    {
        if (failIfNull && (value == null))
        {
            throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull(evaluator,
                                                                                          expression));
        }
        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format("Result of expression: {0}:{1} is: {2}", evaluator, expression,
                                              value));
        }
    }

    public boolean evaluateBoolean(String expression, String evaluator, MuleMessage message)
        throws ExpressionRuntimeException
    {
        return evaluateBoolean(expression, evaluator, message, false, false);
    }

    public boolean evaluateBoolean(String expression, MuleMessage message) throws ExpressionRuntimeException
    {
        return evaluateBoolean(expression, message, false, false);
    }

    public boolean evaluateBoolean(String expression,
                                   String evaluator,
                                   MuleMessage message,
                                   boolean nullReturnsTrue,
                                   boolean nonBooleanReturnsTrue) throws ExpressionRuntimeException
    {
        try
        {
            return resolveBoolean(evaluate(expression, evaluator, message, false), nullReturnsTrue,
                nonBooleanReturnsTrue, expression);
        }
        catch (RequiredValueException e)
        {
            return nullReturnsTrue;
        }
    }

    public boolean evaluateBoolean(String expression,
                                   MuleMessage message,
                                   boolean nullReturnsTrue,
                                   boolean nonBooleanReturnsTrue) throws ExpressionRuntimeException
    {
        try
        {
            return resolveBoolean(evaluate(expression, message, false), nullReturnsTrue,
                nonBooleanReturnsTrue, expression);
        }
        catch (RequiredValueException e)
        {
            return nullReturnsTrue;
        }
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
        if (result == null || NullPayload.getInstance().equals(result))
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

    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate
     * it. If a user needs to evaluate a single expression they can use
     * {@link org.mule.api.expression.ExpressionManager#evaluate(String,org.mule.api.MuleMessage,boolean)}.
     * 
     * @param expression a single expression i.e. xpath://foo
     * @param message the current message to process. The expression will evaluata on the message.
     * @return the result of the evaluation. Expressions that return collection will return an empty
     *         collection, not null.
     * @throws org.mule.api.expression.ExpressionRuntimeException if the expression is invalid, or a null is
     *             found for the expression and 'failIfNull is set to true.
     */
    public String parse(String expression, MuleMessage message) throws ExpressionRuntimeException
    {
        return parse(expression, message, false);
    }

    @Override
    public String parse(String expression, MuleEvent event) throws ExpressionRuntimeException
    {
        return parse(expression, event, false);
    }

    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate
     * it. If a user needs to evaluate a single expression they can use
     * {@link org.mule.api.expression.ExpressionManager#evaluate(String,org.mule.api.MuleMessage,boolean)}.
     * 
     * @param expression a single expression i.e. xpath://foo
     * @param message the current message to process. The expression will evaluata on the message.
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or
     *            returns null.
     * @return the result of the evaluation. Expressions that return collection will return an empty
     *         collection, not null.
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression
     *             and 'failIfNull is set to true.
     */
    public String parse(final String expression, final MuleMessage message, final boolean failIfNull)
        throws ExpressionRuntimeException
    {
        return parser.parse(new TemplateParser.TemplateCallback()
        {
            public Object match(String token)
            {
                Object result = evaluate(token, message, failIfNull);
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
    public String parse(String expression, final MuleEvent event, final boolean failIfNull)
        throws ExpressionRuntimeException
    {
        return parser.parse(new TemplateParser.TemplateCallback()
        {
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

    /**
     * Clears all registered evaluators from the manager.
     */
    public synchronized void clearEvaluators()
    {
        for (Iterator iterator = evaluators.values().iterator(); iterator.hasNext();)
        {
            ExpressionEvaluator evaluator = (ExpressionEvaluator) iterator.next();
            if (evaluator instanceof Disposable)
            {
                ((Disposable) evaluator).dispose();
            }
        }
        evaluators.clear();
    }

    public void clearEnrichers()
    {
        for (Iterator iterator = enrichers.values().iterator(); iterator.hasNext();)
        {
            ExpressionEnricher enricher = (ExpressionEnricher) iterator.next();
            if (enricher instanceof Disposable)
            {
                ((Disposable) enricher).dispose();
            }
        }
        enrichers.clear();
    }

    public boolean isExpression(String string)
    {
        return (string.contains(DEFAULT_EXPRESSION_PREFIX));
    }

    @Override
    public TypedValue evaluateTyped(String expression, MuleMessage message)
    {
        expression = removeExpressionMarker(expression);
        if (isEvaluatorExpression(expression))
        {
            String[] parts = expression.split(":", 2);
            return evaluateTyped(parts[1], parts[0], message, false);
        }
        else
        {
            return expressionLanguage.evaluateTyped(expression, message);
        }
    }

    /**
     * Determines if the expression is valid or not. This method will validate a single expression or
     * expressions embedded in a string. the expression must be well formed i.e. #[bean:user]
     * 
     * @param expression the expression to validate
     * @return true if the expression evaluator is recognised
     */
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
            if (logger.isDebugEnabled())
            {
            	logger.debug("Expression '" + expression + "' is invalid.", e);
            }
            return false;
        }
    }

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
        parser.parse(new TemplateParser.TemplateCallback()
        {
            public Object match(String token)
            {
                match.set(true);
                if (!isEvaluatorExpression(token))
                {
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
                }
                return null;
            }
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
        MVELExpressionLanguage mel = new MVELExpressionLanguage(muleContext);
        mel.initialise();
        expressionLanguage = mel;
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

    protected boolean isEvaluatorExpression(String expression)
    {
        int colonIndex = expression.indexOf(":");
        if (colonIndex < 0)
        {
            return false;
        }
        else
        {
            return evaluators.containsKey(expression.substring(0, colonIndex));
        }
    }

    protected boolean isEnricherExpression(String expression)
    {
        int colonIndex = expression.indexOf(":");
        if (colonIndex < 0)
        {
            return false;
        }
        else
        {
            return enrichers.containsKey(expression.substring(0, colonIndex));
        }
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
