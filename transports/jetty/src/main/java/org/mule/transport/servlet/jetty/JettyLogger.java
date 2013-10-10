/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.mule.module.logging.MuleLoggerFactory;
import org.mule.util.StringMessageUtils;

import java.util.ArrayList;
import java.util.List;

import org.mortbay.log.Logger;
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
        String message = null;
        if ((loggerFactory instanceof MuleLoggerFactory) == false)
        {
            List<String> messages = new ArrayList<String>();
            messages.add("Mule's StaticLoggerBinder should be installed but isn't.");
            messages.add("Logger factory in place is: " + loggerFactory.getClass().getName());
            message = StringMessageUtils.getBoilerPlate(messages, '!', 70);
        }

        logger = loggerFactory.getLogger("org.mortbay.jetty");
        if (message != null)
        {
            logger.warn(message);
        }
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
    public void info(String msg, Object arg0, Object arg1)
    {
        logger.info(msg, arg0, arg1);
    }

    @Override
    public void debug(String msg, Throwable th)
    {
        logger.debug(msg, th);
    }

    @Override
    public void debug(String msg, Object arg0, Object arg1)
    {
        logger.debug(msg, arg0, arg1);
    }

    @Override
    public void warn(String msg, Object arg0, Object arg1)
    {
        logger.warn(msg, arg0, arg1);
    }

    @Override
    public void warn(String msg, Throwable th)
    {
        logger.warn(msg, th);
    }

    @Override
    public Logger getLogger(String name)
    {
        return this;
    }
}
