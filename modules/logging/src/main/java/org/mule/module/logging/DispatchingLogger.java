/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * A solution for logger references saved as static fields. When such logger is declared
 * the log entries end up in the wrong hierarchy/appender.
 */
public class DispatchingLogger implements Logger
{
    protected static final Integer NO_CCL_CLASSLOADER = 0;

    protected Logger originalLogger;
    protected Integer originalClassLoaderHash;
    private String name;
    private MuleLoggerFactory factory;

    public DispatchingLogger(Logger originalLogger, MuleLoggerFactory factory)
    {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        this.originalClassLoaderHash = ccl == null ? NO_CCL_CLASSLOADER : ccl.hashCode();
        this.originalLogger = originalLogger;
        this.name = originalLogger.getName();
        this.factory = factory;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isTraceEnabled()
    {
        return getLogger().isTraceEnabled();
    }

    @Override
    public void trace(String msg)
    {
        getLogger().trace(msg);
    }

    @Override
    public void trace(String format, Object arg)
    {
        getLogger().trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2)
    {
        getLogger().trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object[] argArray)
    {
        getLogger().trace(format, argArray);
    }

    @Override
    public void trace(String msg, Throwable t)
    {
        getLogger().trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker)
    {
        return getLogger().isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg)
    {
        getLogger().trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg)
    {
        getLogger().trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2)
    {
        getLogger().trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object[] argArray)
    {
        getLogger().trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t)
    {
        getLogger().trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled()
    {
        return getLogger().isDebugEnabled();
    }

    @Override
    public void debug(String msg)
    {
        getLogger().debug(msg);
    }

    @Override
    public void debug(String format, Object arg)
    {
        getLogger().debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2)
    {
        getLogger().debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object[] argArray)
    {
        getLogger().debug(format, argArray);
    }

    @Override
    public void debug(String msg, Throwable t)
    {
        getLogger().debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker)
    {
        return getLogger().isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg)
    {
        getLogger().debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg)
    {
        getLogger().debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2)
    {
        getLogger().debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object[] argArray)
    {
        getLogger().debug(marker, format, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t)
    {
        getLogger().debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled()
    {
        return getLogger().isInfoEnabled();
    }

    @Override
    public void info(String msg)
    {
        getLogger().info(msg);
    }

    @Override
    public void info(String format, Object arg)
    {
        getLogger().info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2)
    {
        getLogger().info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object[] argArray)
    {
        getLogger().info(format, argArray);
    }

    @Override
    public void info(String msg, Throwable t)
    {
        getLogger().info(msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker)
    {
        return getLogger().isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg)
    {
        getLogger().info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg)
    {
        getLogger().info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2)
    {
        getLogger().info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object[] argArray)
    {
        getLogger().info(marker, format, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t)
    {
        getLogger().info(marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled()
    {
        return getLogger().isWarnEnabled();
    }

    @Override
    public void warn(String msg)
    {
        getLogger().warn(msg);
    }

    @Override
    public void warn(String format, Object arg)
    {
        getLogger().warn(format, arg);
    }

    @Override
    public void warn(String format, Object[] argArray)
    {
        getLogger().warn(format, argArray);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2)
    {
        getLogger().warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t)
    {
        getLogger().warn(msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker)
    {
        return getLogger().isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg)
    {
        getLogger().warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg)
    {
        getLogger().warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2)
    {
        getLogger().warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object[] argArray)
    {
        getLogger().warn(marker, format, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t)
    {
        getLogger().warn(marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled()
    {
        return getLogger().isErrorEnabled();
    }

    @Override
    public void error(String msg)
    {
        getLogger().error(msg);
    }

    @Override
    public void error(String format, Object arg)
    {
        getLogger().error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2)
    {
        getLogger().error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object[] argArray)
    {
        getLogger().error(format, argArray);
    }

    @Override
    public void error(String msg, Throwable t)
    {
        getLogger().error(msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker)
    {
        return getLogger().isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg)
    {
        getLogger().error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg)
    {
        getLogger().error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2)
    {
        getLogger().error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray)
    {
        getLogger().error(marker, format, argArray);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t)
    {
        getLogger().error(marker, msg, t);
    }

    /**
     * Dispatches lookup to the factory to pick up the right logger based on the context classloader,
     * even if originally the logger was created with another classloader (which is the case with static
     * log refs).
     */
    protected Logger getLogger()
    {
        final ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        if (currentCl == null || currentCl.hashCode() == originalClassLoaderHash)
        {
            return originalLogger;
        }
        // trick - this is probably a logger declared in a static field
        // the classloader used to create it and the TCCL can be different
        // ask factory for the correct instance
        return factory.getLogger(getName(), currentCl);
    }

    public MuleLoggerFactory getFactory()
    {
        return factory;
    }
}
