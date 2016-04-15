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
        logBuffer(buffer);
    }

    @Override
    public void onDataSentEvent(Connection connection, Buffer buffer)
    {
        logBuffer(buffer);
    }

    private void logBuffer(Buffer buffer)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(loggerType.name() + "\n" + buffer.toStringContent());
        }
    }

}
