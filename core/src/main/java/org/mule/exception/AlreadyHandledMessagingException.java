/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.MessagingException;
import org.mule.config.i18n.Message;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * This class is used to wrap any MessagingException already managed by an exception strategy
 * so the same exception strategy do not manage the same exception twice.
 */
public class AlreadyHandledMessagingException extends MessagingException
{
    private MessagingException delegate;

    public AlreadyHandledMessagingException(MessagingException exceptionWrapped)
    {
        super(exceptionWrapped.getI18nMessage(),exceptionWrapped.getEvent(),exceptionWrapped);
        delegate = exceptionWrapped;
    }

    @Override
    public int getExceptionCode()
    {
        return delegate.getExceptionCode();
    }

    @Override
    public Message getI18nMessage()
    {
        return delegate.getI18nMessage();
    }

    @Override
    public int getMessageCode()
    {
        return delegate.getMessageCode();
    }

    @Override
    public void addInfo(String name, Object info)
    {
        delegate.addInfo(name, info);
    }

    @Override
    public String getDetailedMessage()
    {
        return delegate.getDetailedMessage();
    }

    @Override
    public String getVerboseMessage()
    {
        return delegate.getVerboseMessage();
    }

    @Override
    public String getSummaryMessage()
    {
        return delegate.getSummaryMessage();
    }

    @Override
    public boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    @Override
    public Map getInfo()
    {
        return delegate.getInfo();
    }

    @Override
    public String getLocalizedMessage()
    {
        return delegate.getLocalizedMessage();
    }

    @Override
    public Throwable getCause()
    {
        return delegate.getCause();
    }

    @Override
    public Throwable initCause(Throwable throwable)
    {
        return delegate.initCause(throwable);
    }

    @Override
    public String toString()
    {
        return delegate.toString();
    }

    @Override
    public void printStackTrace()
    {
        delegate.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream printStream)
    {
        delegate.printStackTrace(printStream);
    }

    @Override
    public void printStackTrace(PrintWriter printWriter)
    {
        delegate.printStackTrace(printWriter);
    }

    @Override
    public Throwable fillInStackTrace()
    {
        return delegate.fillInStackTrace();
    }

    @Override
    public StackTraceElement[] getStackTrace()
    {
        return delegate.getStackTrace();
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTraceElements)
    {
        delegate.setStackTrace(stackTraceElements);
    }
}
