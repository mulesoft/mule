/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.errorhandler;

import org.mule.util.ClassUtils;

/**
 * The <code>ExceptionBean</code> is a POJO representing the details of a Throwable
 */
public class ExceptionBean
{
    private static boolean showRootStackOnly = true;

    /**
     * Specific details about the Throwable. For example, for
     * <tt>FileNotFoundException</tt>, this contains the name of the file that
     * could not be found.
     */
    private String detailMessage;

    /**
     * The throwable that caused this throwable to get thrown, or null if this
     * throwable was not caused by another throwable, or if the causative throwable
     * is unknown.
     */

    private ExceptionBean cause = null;

    /**
     * The stack trace, as returned by getStackTrace().
     */
    private String[] stackTrace;

    private String exceptionClass = null;

    private Throwable originalException = null;

    public ExceptionBean()
    {
        super();
    }

    public ExceptionBean(Throwable exception)
    {
        if (exception == null) throw new IllegalArgumentException("The exception cannot be null");
        originalException = exception;
        exceptionClass = exception.getClass().getName();
        setDetailMessage(exception.getMessage());
        setStackTrace((showRootStackOnly ? null : getStackAsString(exception.getStackTrace())));
        if (exception.getCause() != null)
        {
            setCause(new ExceptionBean(exception.getCause()));
        }
        else
        {
            setStackTrace(exception.getStackTrace());
        }
    }

    public Throwable toException() throws InstantiationException
    {
        if (originalException == null)
        {
            Throwable t = null;
            try
            {
                Class aClass = ClassUtils.loadClass(exceptionClass, getClass());
                if (cause == null)
                {
                    t = (Throwable)ClassUtils.instanciateClass(aClass, getDetailMessage());
                }
                else
                {
                    t = (Throwable)ClassUtils.instanciateClass(aClass, getDetailMessage(), cause.toException());
                }
                if (getStackTrace() != null)
                {
                    // t.setStackTrace( getStackTrace());
                }
                originalException = t;
            }
            catch (Exception e)
            {
                throw new InstantiationException("Failed to create Exception from ExceptionBean: "
                                                 + e.getMessage());
            }
        }
        return originalException;
    }

    public String getDetailMessage()
    {
        return detailMessage;
    }

    public void setDetailMessage(String detailMessage)
    {
        this.detailMessage = detailMessage;
    }

    public ExceptionBean getCause()
    {
        return cause;
    }

    public void setCause(ExceptionBean cause)
    {
        this.cause = cause;
    }

    public String[] /* List */
    getStackTrace()
    {
        return stackTrace;
    }

    // public void addStackTrace(String trace)
    // {
    // stackTrace.add(trace);
    // }

    public void setStackTrace(StackTraceElement[] stackTrace)
    {
        this.stackTrace = getStackAsString(stackTrace);
    }

    public void setStackTrace(String[] stackTrace)
    {
        this.stackTrace = stackTrace;
    }

    public String getExceptionClass()
    {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass)
    {
        this.exceptionClass = exceptionClass;
    }

    protected String[] getStackAsString(java.lang.StackTraceElement[] elements)
    {
        String[] trace = new String[elements.length];
        for (int i = 0; i < elements.length; i++)
        {
            trace[i] = elements[i].toString();
        }
        return trace;
    }
}
