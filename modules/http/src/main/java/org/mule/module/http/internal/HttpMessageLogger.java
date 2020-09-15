/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.HttpProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.currentThread;
import static org.mule.module.http.internal.listener.grizzly.ResponseStreamingCompletionHandler.MULE_CLASSLOADER;

/**
 * Logger for plain HTTP request and response.
 */
public class HttpMessageLogger extends HttpProbe.Adapter
{

    private static final Logger logger = LoggerFactory.getLogger(HttpMessageLogger.class);

    private final LoggerType loggerType;

    public enum LoggerType
    {
        LISTENER, REQUESTER
    }

    public HttpMessageLogger(final LoggerType loggerType)
    {
        this.loggerType = loggerType;
    }

    @Override
    public void onDataReceivedEvent(Connection connection, Buffer buffer)
    {
        logBuffer(buffer, getClassLoader(connection));
    }

    @Override
    public void onDataSentEvent(Connection connection, Buffer buffer)
    {
        logBuffer(buffer, getClassLoader(connection));
    }

    private void logBuffer(Buffer buffer, ClassLoader classLoader)
    {
        Thread currentThread = currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        boolean isClassLoaderNull = classLoader == null;
        try
        {
            if (!isClassLoaderNull)
            {
                setContextClassLoader(currentThread, originalClassLoader, classLoader);
            }
            if (logger.isDebugEnabled())
            {
                logger.debug(loggerType.name() + "\n" + buffer.toStringContent());
            }
            if (!isClassLoaderNull)
            {
                setContextClassLoader(currentThread, classLoader, originalClassLoader);
            }
        } catch (Exception e)
        {
            if (!isClassLoaderNull)
            {
                setContextClassLoader(currentThread, classLoader, originalClassLoader);
            }
            throw (e);
        }
    }

    private ClassLoader getClassLoader(Connection connection)
    {
        return (connection.getAttributes() != null) ?
                (ClassLoader) connection.getAttributes().getAttribute(MULE_CLASSLOADER) : null;
    }

    private void setContextClassLoader(Thread thread, ClassLoader currentClassLoader, ClassLoader newClassLoader) {
        if (currentClassLoader != newClassLoader && newClassLoader != null) {
            thread.setContextClassLoader(newClassLoader);
        }
    }

}
