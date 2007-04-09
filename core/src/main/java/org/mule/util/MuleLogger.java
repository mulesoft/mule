/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;

/**
 * A {@link Log} wrapper that supports boilerplate logging for high impact messages
 */
// @Immutable
public class MuleLogger implements Log
{
    private final Log logger;

    public MuleLogger(Log logger)
    {
        if (logger == null)
        {
            throw new NullPointerException("logger");
        }

        this.logger = logger;
    }

    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    public boolean isErrorEnabled()
    {
        return logger.isErrorEnabled();
    }

    public boolean isFatalEnabled()
    {
        return logger.isFatalEnabled();
    }

    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    public boolean isTraceEnabled()
    {
        return logger.isTraceEnabled();
    }

    public boolean isWarnEnabled()
    {
        return logger.isWarnEnabled();
    }

    public void trace(Object o)
    {
        logger.trace(o);
    }

    public void trace(Object o, Throwable throwable)
    {
        logger.trace(o, throwable);
    }

    public void debug(Object o)
    {
        logger.debug(o);
    }

    public void debug(Object o, Throwable throwable)
    {
        logger.debug(o, throwable);
    }

    public void info(Object o)
    {
        logger.info(o);
    }

    public void info(Object o, Throwable throwable)
    {
        logger.info(o, throwable);
    }

    public void warn(Object o)
    {
        logger.warn(o);
    }

    public void warn(Object o, Throwable throwable)
    {
        logger.warn(o, throwable);
    }

    public void error(Object o)
    {
        logger.error(o);
    }

    public void error(Object o, Throwable throwable)
    {
        logger.error(o, throwable);
    }

    public void fatal(Object o)
    {
        logger.fatal(o);
    }

    public void fatal(Object o, Throwable throwable)
    {
        logger.fatal(o, throwable);
    }

    public void boilerPlate(String message)
    {
        boilerPlate(message, '*', StringMessageUtils.DEFAULT_MESSAGE_WIDTH);
    }

    public void logBoilerPlate(List messages)
    {
        boilerPlate(messages, '*', StringMessageUtils.DEFAULT_MESSAGE_WIDTH);
    }

    public void logBoilerPlate(String[] messages)
    {
        boilerPlate(Arrays.asList(messages), '*', StringMessageUtils.DEFAULT_MESSAGE_WIDTH);
    }

    public void boilerPlate(String message, char c, int maxlength)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("\n" + StringMessageUtils.getBoilerPlate(message, c, maxlength));
        }
    }

    public void boilerPlate(List messages, char c, int maxlength)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("\n" + StringMessageUtils.getBoilerPlate(messages, c, maxlength));
        }
    }

    public void boilerPlate(String[] messages, char c, int maxlength)
    {
        boilerPlate(Arrays.asList(messages), c, maxlength);
    }

}
