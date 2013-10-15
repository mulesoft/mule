/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.logging;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class MuleLoggerFactory implements ILoggerFactory
{

    public static final String LOG_HANDLER_THREAD_NAME = "Mule.log.slf4j.ref.handler";

    protected static final Integer NO_CCL_CLASSLOADER = 0;

    protected ConcurrentMap<Integer, ConcurrentMap<String, Logger>> repository = new ConcurrentHashMap<Integer, ConcurrentMap<String, Logger>>();

    protected ReferenceQueue<ClassLoader> referenceQueue = new ReferenceQueue<ClassLoader>();
    // map ref back to the classloader hash for cleanup of repository map, as both Weak- and SoftReference's get() return null by this time
    protected Map<PhantomReference<ClassLoader>, Integer> refs = new HashMap<PhantomReference<ClassLoader>, Integer>();

    public MuleLoggerFactory()
    {
        if (MuleUtils.isStandalone())
        {
            createLoggerReferenceHandler();
        }
    }

    protected void createLoggerReferenceHandler()
    {
        new LoggerReferenceHandler(LOG_HANDLER_THREAD_NAME, referenceQueue, refs, repository);
    }

    @Override
    public Logger getLogger(String name)
    {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        return getLogger(name, ccl);
    }

    public Logger getLogger(String name, ClassLoader classLoader)
    {
        ConcurrentMap<String, Logger> loggerMap = repository.get(classLoader == null ? NO_CCL_CLASSLOADER : classLoader.hashCode());

        if (loggerMap == null)
        {
            loggerMap = new ConcurrentHashMap<String, Logger>();
            final ConcurrentMap<String, Logger> previous = repository.putIfAbsent(classLoader == null ? NO_CCL_CLASSLOADER : classLoader.hashCode(), loggerMap);
            if (previous != null)
            {
                loggerMap = previous;
            }

            if (classLoader != null)
            {
                // must save a strong ref to the PhantomReference in order for it to stay alive and work
                refs.put(new PhantomReference<ClassLoader>(classLoader, referenceQueue), classLoader.hashCode());
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
