/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.core.api.util.StreamingUtils.withCursoredEvent;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.StringUtils;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageProcessor implementation that logs the current element of a value evaluated from it using an expression evaluator. By
 * default the current messages is logged using the {@link Level#DEBUG} level to the
 * 'org.mule.runtime.core.internal.processor.LoggerMessageProcessor' category. The level and category can both be configured to suit
 * your needs.
 */
public class LoggerMessageProcessor extends AbstractComponent implements Processor, Initialisable, MuleContextAware {

  protected transient Logger logger;

  protected String message;
  protected String category;
  protected String level = "INFO";

  protected MuleContext muleContext;
  ExtendedExpressionManager expressionManager;

  @Override
  public void initialise() throws InitialisationException {
    initLogger();
    expressionManager = muleContext.getExpressionManager();
  }

  protected void initLogger() {
    if (category != null) {
      logger = LoggerFactory.getLogger(category);
    } else {
      logger = LoggerFactory.getLogger(LoggerMessageProcessor.class);
    }
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return withCursoredEvent(event, cursored -> {
      log(cursored);
      return event;
    });
  }

  protected void log(CoreEvent event) {
    if (event == null) {
      logWithLevel(null);
    } else {
      if (StringUtils.isEmpty(message)) {
        logWithLevel(event.getMessage());
      } else {
        LogLevel logLevel = LogLevel.valueOf(level);
        if (LogLevel.valueOf(level).isEnabled(logger)) {
          logLevel.log(logger, expressionManager.parseLogTemplate(message, event, getLocation(), NULL_BINDING_CONTEXT));
        }
      }
    }
  }

  protected void logWithLevel(Object object) {
    LogLevel logLevel = LogLevel.valueOf(level);
    if (logLevel.isEnabled(logger)) {
      logLevel.log(logger, object);
    }
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public void setLevel(String level) {
    this.level = level.toUpperCase();
  }

  public enum LogLevel {
    ERROR {

      @Override
      public void log(Logger logger, Object object) {
        logger.error(object == null ? null : object.toString());
      }

      @Override
      public boolean isEnabled(Logger logger) {
        return logger.isErrorEnabled();
      }
    },
    WARN {

      @Override
      public void log(Logger logger, Object object) {
        logger.warn(object == null ? null : object.toString());
      }

      @Override
      public boolean isEnabled(Logger logger) {
        return logger.isWarnEnabled();
      }
    },
    INFO {

      @Override
      public void log(Logger logger, Object object) {
        logger.info(object == null ? null : object.toString());
      }

      @Override
      public boolean isEnabled(Logger logger) {
        return logger.isInfoEnabled();
      }
    },
    DEBUG {

      @Override
      public void log(Logger logger, Object object) {
        logger.debug(object == null ? null : object.toString());
      }

      @Override
      public boolean isEnabled(Logger logger) {
        return logger.isDebugEnabled();
      }
    },
    TRACE {

      @Override
      public void log(Logger logger, Object object) {
        logger.trace(object == null ? null : object.toString());
      }

      @Override
      public boolean isEnabled(Logger logger) {
        return logger.isTraceEnabled();
      }
    };

    public abstract void log(Logger logger, Object object);

    public abstract boolean isEnabled(Logger logger);
  }
}
