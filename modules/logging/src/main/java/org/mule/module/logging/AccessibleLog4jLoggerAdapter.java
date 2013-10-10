/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.logging;

import java.io.Serializable;

import org.apache.log4j.Level;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

/**
 * A copy & paste of the {@link AccessibleLog4jLoggerAdapter}, just making it
 * non-final.
 */
public class AccessibleLog4jLoggerAdapter extends MarkerIgnoringBase implements LocationAwareLogger, Serializable
{

    private static final long serialVersionUID = 6182834493563598289L;

    final transient org.apache.log4j.Logger logger;

    /**
     * Following the pattern discussed in pages 162 through 168 of "The complete
     * log4j manual".
     */
    final static String FQCN = AccessibleLog4jLoggerAdapter.class.getName();

    // Does the log4j version in use recognize the TRACE level?
    // The trace level was introduced in log4j 1.2.12.
    final boolean traceCapable;

    // WARN: Log4jLoggerAdapter constructor should have only package access so
    // that
    // only Log4jLoggerFactory be able to create one.
    AccessibleLog4jLoggerAdapter(org.apache.log4j.Logger logger)
    {
        this.logger = logger;
        this.name = logger.getName();
        traceCapable = isTraceCapable();
    }

    private boolean isTraceCapable()
    {
        try
        {
            logger.isTraceEnabled();
            return true;
        }
        catch (NoSuchMethodError e)
        {
            return false;
        }
    }

    /**
     * Is this logger instance enabled for the TRACE level?
     *
     * @return True if this Logger is enabled for level TRACE, false otherwise.
     */
    public boolean isTraceEnabled()
    {
        if (traceCapable)
        {
            return logger.isTraceEnabled();
        }
        else
        {
            return logger.isDebugEnabled();
        }
    }

    /**
     * Log a message object at level TRACE.
     *
     * @param msg - the message object to be logged
     */
    public void trace(String msg)
    {
        logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, msg, null);
    }

    /**
     * Log a message at level TRACE according to the specified format and
     * argument.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for level TRACE.
     * </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public void trace(String format, Object arg)
    {
        if (isTraceEnabled())
        {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
                    .getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level TRACE according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the TRACE level.
     * </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void trace(String format, Object arg1, Object arg2)
    {
        if (isTraceEnabled())
        {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
                    .getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level TRACE according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the TRACE level.
     * </p>
     *
     * @param format   the format string
     * @param argArray an array of arguments
     */
    public void trace(String format, Object[] argArray)
    {
        if (isTraceEnabled())
        {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
                    .getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level TRACE with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public void trace(String msg, Throwable t)
    {
        logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, msg, t);
    }

    /**
     * Is this logger instance enabled for the DEBUG level?
     *
     * @return True if this Logger is enabled for level DEBUG, false otherwise.
     */
    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    /**
     * Log a message object at level DEBUG.
     *
     * @param msg - the message object to be logged
     */
    public void debug(String msg)
    {
        logger.log(FQCN, Level.DEBUG, msg, null);
    }

    /**
     * Log a message at level DEBUG according to the specified format and
     * argument.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for level DEBUG.
     * </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public void debug(String format, Object arg)
    {
        if (logger.isDebugEnabled())
        {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level DEBUG according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the DEBUG level.
     * </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void debug(String format, Object arg1, Object arg2)
    {
        if (logger.isDebugEnabled())
        {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level DEBUG according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the DEBUG level.
     * </p>
     *
     * @param format   the format string
     * @param argArray an array of arguments
     */
    public void debug(String format, Object[] argArray)
    {
        if (logger.isDebugEnabled())
        {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level DEBUG with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public void debug(String msg, Throwable t)
    {
        logger.log(FQCN, Level.DEBUG, msg, t);
    }

    /**
     * Is this logger instance enabled for the INFO level?
     *
     * @return True if this Logger is enabled for the INFO level, false otherwise.
     */
    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    /**
     * Log a message object at the INFO level.
     *
     * @param msg - the message object to be logged
     */
    public void info(String msg)
    {
        logger.log(FQCN, Level.INFO, msg, null);
    }

    /**
     * Log a message at level INFO according to the specified format and argument.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     * </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public void info(String format, Object arg)
    {
        if (logger.isInfoEnabled())
        {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     * </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void info(String format, Object arg1, Object arg2)
    {
        if (logger.isInfoEnabled())
        {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level INFO according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     * </p>
     *
     * @param format   the format string
     * @param argArray an array of arguments
     */
    public void info(String format, Object[] argArray)
    {
        if (logger.isInfoEnabled())
        {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the INFO level with an accompanying
     * message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public void info(String msg, Throwable t)
    {
        logger.log(FQCN, Level.INFO, msg, t);
    }

    /**
     * Is this logger instance enabled for the WARN level?
     *
     * @return True if this Logger is enabled for the WARN level, false otherwise.
     */
    public boolean isWarnEnabled()
    {
        return logger.isEnabledFor(Level.WARN);
    }

    /**
     * Log a message object at the WARN level.
     *
     * @param msg - the message object to be logged
     */
    public void warn(String msg)
    {
        logger.log(FQCN, Level.WARN, msg, null);
    }

    /**
     * Log a message at the WARN level according to the specified format and
     * argument.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.
     * </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public void warn(String format, Object arg)
    {
        if (logger.isEnabledFor(Level.WARN))
        {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the WARN level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.
     * </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void warn(String format, Object arg1, Object arg2)
    {
        if (logger.isEnabledFor(Level.WARN))
        {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level WARN according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.
     * </p>
     *
     * @param format   the format string
     * @param argArray an array of arguments
     */
    public void warn(String format, Object[] argArray)
    {
        if (logger.isEnabledFor(Level.WARN))
        {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the WARN level with an accompanying
     * message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public void warn(String msg, Throwable t)
    {
        logger.log(FQCN, Level.WARN, msg, t);
    }

    /**
     * Is this logger instance enabled for level ERROR?
     *
     * @return True if this Logger is enabled for level ERROR, false otherwise.
     */
    public boolean isErrorEnabled()
    {
        return logger.isEnabledFor(Level.ERROR);
    }

    /**
     * Log a message object at the ERROR level.
     *
     * @param msg - the message object to be logged
     */
    public void error(String msg)
    {
        logger.log(FQCN, Level.ERROR, msg, null);
    }

    /**
     * Log a message at the ERROR level according to the specified format and
     * argument.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.
     * </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public void error(String format, Object arg)
    {
        if (logger.isEnabledFor(Level.ERROR))
        {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the ERROR level according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.
     * </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void error(String format, Object arg1, Object arg2)
    {
        if (logger.isEnabledFor(Level.ERROR))
        {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level ERROR according to the specified format and
     * arguments.
     * <p/>
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.
     * </p>
     *
     * @param format   the format string
     * @param argArray an array of arguments
     */
    public void error(String format, Object[] argArray)
    {
        if (logger.isEnabledFor(Level.ERROR))
        {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying
     * message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public void error(String msg, Throwable t)
    {
        logger.log(FQCN, Level.ERROR, msg, t);
    }

    public void log(Marker marker, String callerFQCN, int level, String msg,
                    Object[] argArray, Throwable t)
    {
        Level log4jLevel;
        switch (level)
        {
            case LocationAwareLogger.TRACE_INT:
                log4jLevel = traceCapable ? Level.TRACE : Level.DEBUG;
                break;
            case LocationAwareLogger.DEBUG_INT:
                log4jLevel = Level.DEBUG;
                break;
            case LocationAwareLogger.INFO_INT:
                log4jLevel = Level.INFO;
                break;
            case LocationAwareLogger.WARN_INT:
                log4jLevel = Level.WARN;
                break;
            case LocationAwareLogger.ERROR_INT:
                log4jLevel = Level.ERROR;
                break;
            default:
                throw new IllegalStateException("Level number " + level
                                                + " is not recognized.");
        }
        logger.log(callerFQCN, log4jLevel, msg, t);
    }

}
