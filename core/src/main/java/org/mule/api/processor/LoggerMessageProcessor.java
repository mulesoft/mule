/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;

/**
 * MessageProcessor implementation that logs the current element of a value evaluated
 * from it using an expression evaluator. By default the current messages is logged
 * using the {@link Level#DEBUG} level to the
 * 'org.mule.api.processor.LoggerMessageProcessor' category but the level and
 * category can both be configured to suit your needs.
 */
public class LoggerMessageProcessor implements MessageProcessor, Initialisable, MuleContextAware
{

    protected transient Log logger;

    protected String expression;
    protected String category;
    protected String levelString;
    protected Level level;

    protected MuleContext muleContext;
    protected ExpressionManager expressionManager;

    public void initialise() throws InitialisationException
    {
        if (category != null)
        {
            logger = LogFactory.getLog(category);
        }
        else
        {
            logger = LogFactory.getLog(LoggerMessageProcessor.class);
        }
        level = Level.toLevel(levelString);

        expressionManager = muleContext.getExpressionManager();

    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        log(event);
        return event;
    }

    protected void log(MuleEvent event)
    {
        if (event == null)
        {
            logWithLevel(null, level);
        }
        else
        {
            logWithLevel(expressionManager.evaluate(expression, event.getMessage()), level);
        }
    }

    protected void logWithLevel(Object object, Level level)
    {
        switch (level.toInt())
        {
            case Level.ERROR_INT :
                logger.error(object);
            case Level.WARN_INT :
                logger.warn(object);
            case Level.INFO_INT :
                logger.info(object);
            case Level.DEBUG_INT :
                logger.debug(object);
            case Level.TRACE_INT :
                logger.trace(object);
        }

    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public void setLevelString(String levelString)
    {
        this.levelString = levelString;
    }

}
