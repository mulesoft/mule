/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.logging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class MuleLoggerFactory implements ILoggerFactory
{
    protected ConcurrentMap<Integer, ConcurrentMap<String, Logger>> repository = new ConcurrentHashMap<Integer, ConcurrentMap<String, Logger>>();

    public Logger getLogger(String name)
    {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        return getLogger(name, ccl);
    }

    public Logger getLogger(String name, ClassLoader classLoader)
    {
        ConcurrentMap<String, Logger> loggerMap = repository.get(classLoader.hashCode());

        if (loggerMap == null)
        {
            loggerMap = new ConcurrentHashMap<String, Logger>();
            final ConcurrentMap<String, Logger> previous = repository.putIfAbsent(classLoader.hashCode(), loggerMap);
            if (previous != null)
            {
                loggerMap = previous;
            }
        }

        Logger slf4jLogger = loggerMap.get(name);

        if (slf4jLogger == null)
        {
            org.apache.log4j.Logger log4jLogger;
            if (name.equalsIgnoreCase(Logger.ROOT_LOGGER_NAME))
            {
                log4jLogger = LogManager.getRootLogger();
            }
            else
            {
                log4jLogger = LogManager.getLogger(name);
            }
            slf4jLogger = new DispatchingLogger(new AccessibleLog4jLoggerAdapter(log4jLogger), this);
            final Logger previous = loggerMap.putIfAbsent(name, slf4jLogger);
            if (previous != null)
            {
                // someone got there before us
                slf4jLogger = previous;
            }
        }

        return slf4jLogger;
    }
}
