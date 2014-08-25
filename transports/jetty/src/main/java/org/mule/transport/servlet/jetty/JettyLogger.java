/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.eclipse.jetty.util.log.Logger;
import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

public class JettyLogger implements Logger
{
    private org.slf4j.Logger logger;

    public JettyLogger()
    {
        super();
        initLogger();
    }

    protected void initLogger()
    {
        ILoggerFactory loggerFactory = StaticLoggerBinder.getSingleton().getLoggerFactory();
        logger = loggerFactory.getLogger("org.eclipse.jetty");
    }

    @Override
    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    @Override
    public void setDebugEnabled(boolean enabled)
    {
        warn("Ignoring call to unsupported method 'setDebugEnabled'", null, null);
    }

    @Override
    public Logger getLogger(String name)
    {
        return this;
    }

    @Override
    public void info(Throwable thrown)
    {
        logger.info(thrown.getMessage(), thrown);
    }

    @Override
    public void info(String msg, Object... args)
    {
        logger.info(msg, args);
    }

    @Override
    public void info(String msg, Throwable thrown)
    {
        logger.info(msg, thrown);
    }

    @Override
    public void debug(String msg, Throwable thrown)
    {
        logger.debug(msg, thrown);
    }

    @Override
    public void debug(Throwable thrown)
    {
        logger.debug(thrown.getMessage(), thrown);
    }

    @Override
    public void debug(String msg, Object... args)
    {
        logger.debug(msg, args);
    }

    @Override
    public void warn(String msg, Throwable thrown)
    {
        logger.warn(msg, thrown);
    }

    @Override
    public void warn(Throwable thrown)
    {
        logger.warn(thrown.getMessage(), thrown);
    }

    @Override
    public void warn(String msg, Object... args)
    {
        logger.warn(msg, args);
    }

    @Override
    public void ignore(Throwable thrown)
    {
    }

    @Override
    public String getName()
    {
        return logger.getName();
    }

}
