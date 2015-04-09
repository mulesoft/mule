/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import org.mule.AbstractAnnotatedObject;
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
public class LoggerMessageProcessor extends AbstractAnnotatedObject implements MessageProcessor, Initialisable, MuleContextAware
{
    protected transient Log logger;

    protected String message;
    protected String category;
    protected String level = "DEBUG";

    protected MuleContext muleContext;
    protected ExpressionManager expressionManager;

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
                LogLevel logLevel = LogLevel.valueOf(level);
                if (LogLevel.valueOf(level).isEnabled(logger)) 
                {
                    logLevel.log(logger, expressionManager.parse(message, event));
                }
            }
        }
    }

    protected void logWithLevel(Object object)
    {
        LogLevel logLevel = LogLevel.valueOf(level);
        if (logLevel.isEnabled(logger)) 
        {
            logLevel.log(logger, object);
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

    public enum LogLevel
    {
        ERROR 
        {
            @Override
            public void log(Log logger, Object object) 
            {
                logger.error(object);
            }

            @Override
            public boolean isEnabled(Log logger) 
            {
                return logger.isErrorEnabled();
            }
        },
        WARN 
        {
            @Override
            public void log(Log logger, Object object) 
            {
                logger.warn(object);
            }
            
            @Override
            public boolean isEnabled(Log logger) 
            {
                return logger.isWarnEnabled();
            }
        },
        INFO
        {
            @Override
            public void log(Log logger, Object object) 
            {
                logger.info(object);
            }
            
            @Override
            public boolean isEnabled(Log logger) 
            {
                return logger.isInfoEnabled();
            }
        },
        DEBUG
        {
            @Override
            public void log(Log logger, Object object) 
            {
                logger.debug(object);
            }
            
            @Override
            public boolean isEnabled(Log logger) 
            {
                return logger.isDebugEnabled();
            }
        },
        TRACE
        {
            @Override
            public void log(Log logger, Object object) 
            {
                logger.trace(object);
            }
            
            @Override
            public boolean isEnabled(Log logger) 
            {
                return logger.isTraceEnabled();
            }
        };
        
        public abstract void log(Log logger, Object object);
        public abstract boolean isEnabled(Log logger);
    }
}
