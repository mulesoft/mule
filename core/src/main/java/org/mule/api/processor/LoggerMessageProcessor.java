/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;

/**
 * MessageProcessor implementation that logs the current element of a value evaluated from it using
 * an expression evaluator. By default the current messages is logged using the {@link Level#DEBUG}
 * level to the 'org.mule.api.processor.LoggerMessageProcessor' category. The level and
 * category can both be configured to suit your needs.
 */
public class LoggerMessageProcessor implements MessageProcessor, Initialisable, MuleContextAware, AnnotatedObject
{
    protected transient Log logger;

    protected String message;
    protected String category;
    protected String level = "DEBUG";

    protected MuleContext muleContext;
    protected ExpressionManager expressionManager;
    private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();

    public void initialise() throws InitialisationException
    {
        initLogger();
        expressionManager = muleContext.getExpressionManager();
    }

    protected void initLogger()
    {
        if (category != null)
        {
            logger = LogFactory.getLog(category);
        }
        else
        {
            logger = LogFactory.getLog(LoggerMessageProcessor.class);
        }
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
            logWithLevel(null);
        }
        else
        {
            if (StringUtils.isEmpty(message))
            {
                logWithLevel(event.getMessage());
            }
            else
            {
                logWithLevel(expressionManager.parse(message, event));
            }
        }
    }

    protected void logWithLevel(Object object)
    {
        if ("ERROR".equals(level))
        {
            logger.error(object);
        }
        else if ("WARN".equals(level))
        {
            logger.warn(object);
        }
        else if ("INFO".equals(level))
        {
            if (logger.isInfoEnabled())
            {
                logger.info(object);
            }
        }
        else if ("DEBUG".equals(level))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(object);
            }
        }
        else if ("TRACE".equals(level))
        {
            if (logger.isTraceEnabled())
            {
                logger.trace(object);
            }
        }
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public void setLevel(String level)
    {
        this.level = level.toUpperCase();
    }

    public final Object getAnnotation(QName name)
    {
        return annotations.get(name);
    }

    public final Map<QName, Object> getAnnotations()
    {
        return Collections.unmodifiableMap(annotations);
    }

    public synchronized final void setAnnotations(Map<QName, Object> newAnnotations)
    {
        annotations.clear();
        annotations.putAll(newAnnotations);
    }
}
