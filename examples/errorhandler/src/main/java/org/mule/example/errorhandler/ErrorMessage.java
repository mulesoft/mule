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
    private Map properties = new HashMap();

    /**
     *
     */
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

    public ErrorMessage(Throwable exception, Map props)
    {
        setThrowable(exception);
        setProperties(props);
    }

    /**
     * @return Returns the exception.
     */
    public ExceptionBean getException()
    {
        return exception;
    }

    /**
     * @param exception The exception to set.
     */
    public void setException(ExceptionBean exception) throws InstantiationException
    {
        this.exception = exception;
        throwable = exception.toException();
    }

    /**
     * @return Returns the properties.
     */
    public Map getProperties()
    {
        return properties;
    }

    /**
     * @param properties The properties to set.
     */
    public void setProperties(Map properties)
    {
        this.properties = properties;
    }

    /**
     * @return Returns the throwable.
     */
    public Throwable getThrowable()
    {
        return throwable;
    }

    /**
     * @param throwable The throwable to set.
     */
    public void setThrowable(Throwable throwable)
    {
        this.throwable = throwable;
        exception = new ExceptionBean(throwable);
    }

}
