/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.errorhandler;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>ErrorMessage</code> represents an exception.
 */
public class ErrorMessage
{
    // Bean representing the Exception
    private ExceptionBean exception;

    // The exception itself, in its primitive state
    private Throwable throwable;

    // Properties for this object
    private Map<Object, Object> properties = new HashMap<Object, Object>();

    public ErrorMessage()
    {
        super();
    }

    public ErrorMessage(ExceptionBean exception) throws InstantiationException
    {
        setException(exception);
    }

    public ErrorMessage(Throwable exception)
    {
        setThrowable(exception);
    }

    public ErrorMessage(Throwable exception, Map<Object, Object> props)
    {
        setThrowable(exception);
        setProperties(props);
    }

    public ExceptionBean getException()
    {
        return exception;
    }

    public void setException(ExceptionBean exception) throws InstantiationException
    {
        this.exception = exception;
        throwable = exception.toException();
    }

    public Map<Object, Object> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<Object, Object> properties)
    {
        this.properties = properties;
    }

    public Throwable getThrowable()
    {
        return throwable;
    }

    public void setThrowable(Throwable throwable)
    {
        this.throwable = throwable;
        exception = new ExceptionBean(throwable);
    }
}
